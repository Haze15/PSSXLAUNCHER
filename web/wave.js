/*
 * PSSXLAUNCHER — XMB ribbon background (WebGL 1).
 * Shader adapted from OpenXMB shaders/wave.vert + wave.frag, themselves adapted from
 * RetroArch gfx/drivers/vulkan_shaders/pipeline_ribbon.{vert,frag}.
 * Copyright (C) 2010-2014 Hans-Kristian Arntzen, (C) 2011-2021 Daniel De Matteis and contributors.
 * Licensed under GPLv3 — see LICENSE and LICENSE.RetroArch.
 */
'use strict';
window.Wave = (function () {
  // Same ribbon as RetroArch/OpenXMB, but the lighting is computed per vertex from the surface normal
  // (finite differences) instead of per pixel with dFdx/dFdy: smooth instead of faceted, and much cheaper
  // for the TV box GPU (the fragment shader only does a mix).
  const VS = `
attribute vec2 VertexCoord;
uniform float time;
uniform vec4 color;
varying vec3 vCol;
varying float vA;
float iqhash(float n) { return fract(sin(n) * 43758.546875); }
float _noise(vec3 x) {
  vec3 p = floor(x);
  vec3 f = fract(x);
  f = (f * f) * (vec3(3.0) - (f * 2.0));
  float n = (p.x + (p.y * 57.0)) + (113.0 * p.z);
  return mix(mix(mix(iqhash(n), iqhash(n + 1.0), f.x), mix(iqhash(n + 57.0), iqhash(n + 58.0), f.x), f.y),
             mix(mix(iqhash(n + 113.0), iqhash(n + 114.0), f.x), mix(iqhash(n + 170.0), iqhash(n + 171.0), f.x), f.y), f.z);
}
vec3 ribbon(vec2 c) {
  float t = time * 0.82;
  vec3 v = vec3(c.x * 1.08, 0.0, c.y * 0.92);
  vec3 v3 = v;
  v.y = cos(v.z * 3.4) * cos((v.z + (t / 12.0)) + v.x * 0.92) / 10.5;
  v3.x -= (t / 6.4);
  v3.x /= 5.2;
  v3.z -= (t / 12.0);
  v3.y -= (t / 110.0);
  float nz = _noise(v3 * 6.2);
  v.z -= nz / 18.0;
  v.y -= ((nz / 20.0) + (cos((v.x * 1.65) - (t / 2.8)) / 8.5)) - 0.245;
  return v;
}
void main() {
  vec3 v = ribbon(VertexCoord);
  vec3 tx = ribbon(VertexCoord + vec2(0.02, 0.0)) - v;
  vec3 ty = ribbon(VertexCoord + vec2(0.0, 0.06)) - v;
  vec3 normal = normalize(cross(tx, ty));
  float c = 1.0 - abs(normal.z);
  c = (1.0 - cos(c * c)) / 3.0;
  c = smoothstep(0.004, 0.14, c);
  float depthFade = smoothstep(-0.92, -0.52, v.z) * (1.0 - smoothstep(0.56, 0.94, v.z));
  float horizonFade = smoothstep(-0.08, 0.34, v.y) * (1.0 - smoothstep(0.58, 0.84, v.y));
  float edge = smoothstep(1.0, 0.82, abs(VertexCoord.y)) * smoothstep(1.0, 0.9, abs(VertexCoord.x));
  float intensity = (0.10 + 0.9 * c) * mix(0.52, 1.0, depthFade) * mix(0.72, 1.0, horizonFade) * edge;
  vec3 tint = mix(color.rgb, vec3(1.0), 0.58);
  vCol = mix(tint, vec3(1.0), smoothstep(0.55, 1.0, intensity)) * intensity * 0.74;
  vA = intensity * 0.55;
  gl_Position = vec4(v, 1.0);
}`;
  const FS = `
precision mediump float;
varying vec3 vCol;
varying float vA;
void main() { gl_FragColor = vec4(vCol, vA); }`;

  let gl, prog, uTime, uColor, nIdx, raf = 0, t0 = performance.now(), last = 0, color = [1, 1, 1], running = false;

  function compile(type, src) {
    const s = gl.createShader(type);
    gl.shaderSource(s, src); gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  }

  function init(canvas) {
    try {
      gl = canvas.getContext('webgl', {alpha: true, premultipliedAlpha: true, antialias: true, depth: false, powerPreference: 'low-power'})
        || canvas.getContext('experimental-webgl');
      if (!gl) return false;
      prog = gl.createProgram();
      gl.attachShader(prog, compile(gl.VERTEX_SHADER, VS));
      gl.attachShader(prog, compile(gl.FRAGMENT_SHADER, FS));
      gl.linkProgram(prog);
      if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(prog));
      gl.useProgram(prog);
      // grid mesh, like RetroArch's ribbon
      const NX = 120, NY = 28, verts = [], idx = [];
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
    if (now - last < 40) return;          // 25 fps: smooth enough for a slow wave, light on the TV box
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
