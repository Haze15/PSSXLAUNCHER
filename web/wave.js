/*
 * PSSXLAUNCHER — XMB ribbon background (WebGL 1).
 * Shader adapted from OpenXMB shaders/wave.vert + wave.frag, themselves adapted from
 * RetroArch gfx/drivers/vulkan_shaders/pipeline_ribbon.{vert,frag}.
 * Copyright (C) 2010-2014 Hans-Kristian Arntzen, (C) 2011-2021 Daniel De Matteis and contributors.
 * Licensed under GPLv3 — see LICENSE and LICENSE.RetroArch.
 */
'use strict';
window.Wave = (function () {
  const VS = `
attribute vec2 VertexCoord;
uniform float time;
varying vec3 vEC;
float xmb_noise2(vec3 x) {
  float t = time * 0.82;
  return cos(x.z * 3.4) * cos((x.z + (t / 12.0)) + x.x * 0.92);
}
float iqhash(float n) { return fract(sin(n) * 43758.546875); }
float _noise(vec3 x) {
  vec3 p = floor(x);
  vec3 f = fract(x);
  f = (f * f) * (vec3(3.0) - (f * 2.0));
  float n = (p.x + (p.y * 57.0)) + (113.0 * p.z);
  return mix(mix(mix(iqhash(n), iqhash(n + 1.0), f.x), mix(iqhash(n + 57.0), iqhash(n + 58.0), f.x), f.y),
             mix(mix(iqhash(n + 113.0), iqhash(n + 114.0), f.x), mix(iqhash(n + 170.0), iqhash(n + 171.0), f.x), f.y), f.z);
}
void main() {
  float t = time * 0.82;
  vec3 v = vec3(VertexCoord.x * 1.08, 0.0, VertexCoord.y * 0.92);
  vec3 v3 = v;
  v.y = xmb_noise2(v) / 10.5;
  v3.x -= (t / 6.4);
  v3.x /= 5.2;
  v3.z -= (t / 12.0);
  v3.y -= (t / 110.0);
  v.z -= (_noise(v3 * 6.2) / 18.0);
  v.y -= (((_noise(v3 * 6.2) / 20.0) + (cos((v.x * 1.65) - (t / 2.8)) / 8.5)) - 0.245);
  vEC = v;
  gl_Position = vec4(v, 1.0);
}`;
  const FS_DERIV = `
#extension GL_OES_standard_derivatives : enable
precision mediump float;
uniform vec4 color;
varying vec3 vEC;
void main() {
  vec3 x = dFdx(vEC);
  vec3 y = dFdy(vEC);
  vec3 normal = normalize(cross(x, y));
  float c = 1.0 - dot(normal, vec3(0.0, 0.0, 1.0));
  c = (1.0 - cos(c * c)) / 3.0;
  c = smoothstep(0.015, 0.36, c);
  float depthFade = smoothstep(-0.92, -0.52, vEC.z) * (1.0 - smoothstep(0.56, 0.94, vEC.z));
  float horizonFade = smoothstep(-0.08, 0.34, vEC.y) * (1.0 - smoothstep(0.58, 0.84, vEC.y));
  float intensity = c * mix(0.52, 1.0, depthFade) * mix(0.72, 1.0, horizonFade);
  vec3 tint = mix(color.rgb, vec3(1.0), 0.58);
  vec3 hot = mix(tint, vec3(1.0), smoothstep(0.55, 1.0, intensity));
  gl_FragColor = vec4(hot * intensity * 0.74, intensity * 0.55);
}`;
  // No derivatives on this GPU: a softer, flat ribbon
  const FS_FLAT = `
precision mediump float;
uniform vec4 color;
varying vec3 vEC;
void main() {
  float depthFade = smoothstep(-0.92, -0.52, vEC.z) * (1.0 - smoothstep(0.56, 0.94, vEC.z));
  float a = 0.16 * depthFade;
  vec3 tint = mix(color.rgb, vec3(1.0), 0.7);
  gl_FragColor = vec4(tint * a, a);
}`;

  let gl, prog, uTime, uColor, nIdx, raf = 0, t0 = performance.now(), last = 0, color = [1, 1, 1], running = false;

  function compile(type, src) {
    const s = gl.createShader(type);
    gl.shaderSource(s, src); gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  }

  function init(canvas) {
    try {
      gl = canvas.getContext('webgl', {alpha: true, premultipliedAlpha: true, antialias: false, depth: false, powerPreference: 'low-power'})
        || canvas.getContext('experimental-webgl');
      if (!gl) return false;
      const deriv = gl.getExtension('OES_standard_derivatives');
      prog = gl.createProgram();
      gl.attachShader(prog, compile(gl.VERTEX_SHADER, VS));
      gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, deriv ? FS_DERIV : FS_FLAT));
      gl.linkProgram(prog);
      if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(prog));
      gl.useProgram(prog);
      // grid mesh, like RetroArch's ribbon
      const NX = 96, NY = 32, verts = [], idx = [];
      for (let j = 0; j <= NY; j++) for (let i = 0; i <= NX; i++) verts.push(i / NX * 2 - 1, j / NY * 2 - 1);
      for (let j = 0; j < NY; j++) for (let i = 0; i < NX; i++) {
        const a = j * (NX + 1) + i, b = a + 1, c = a + NX + 1, d = c + 1;
        idx.push(a, c, b, b, c, d);
      }
      gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer());
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(verts), gl.STATIC_DRAW);
      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, gl.createBuffer());
      gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(idx), gl.STATIC_DRAW);
      nIdx = idx.length;
      const loc = gl.getAttribLocation(prog, 'VertexCoord');
      gl.enableVertexAttribArray(loc);
      gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
      uTime = gl.getUniformLocation(prog, 'time');
      uColor = gl.getUniformLocation(prog, 'color');
      gl.disable(gl.DEPTH_TEST); gl.disable(gl.CULL_FACE);
      gl.enable(gl.BLEND); gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
      return true;
    } catch (e) { console.log('wave: ' + e.message); gl = null; return false; }
  }

  function frame(now) {
    raf = requestAnimationFrame(frame);
    if (now - last < 32) return;          // ~30 fps is plenty and spares the TV box
    last = now;
    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight);
    gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT);
    gl.uniform1f(uTime, (now - t0) / 1000 * 1.0);
    gl.uniform4f(uColor, color[0], color[1], color[2], 1);
    gl.drawElements(gl.TRIANGLES, nIdx, gl.UNSIGNED_SHORT, 0);
  }

  return {
    init,
    setColor(rgb) { color = rgb; },
    start() { if (gl && !running) { running = true; raf = requestAnimationFrame(frame); } },
    stop() { running = false; cancelAnimationFrame(raf); if (gl) { gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT); } },
    ok: () => !!gl
  };
})();
