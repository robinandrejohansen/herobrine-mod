/*
 * HEROBRINE — the site's script. No framework, no build step, no dependencies.
 *
 * The one thing on this page that is genuinely him: the sprites are composed
 * from the mod's real skin files — front faces of head, body, arms and legs,
 * plus the overlay layer — drawn onto a canvas without smoothing. So the man on
 * the page is the man in the game, pixel for pixel. The eyes are the mod's own
 * emissive layer, drawn on top with a glow. Level three is the same sprite with
 * the light taken out of it, which is exactly what the mod does.
 *
 * Everything animated pauses when it is off-screen or the tab is hidden, caps
 * device-pixel-ratio at two, and draws cached sprites — a phone should not warm
 * up looking at this.
 */

const REDUCED = matchMedia('(prefers-reduced-motion: reduce)').matches;
const DPR = Math.min(window.devicePixelRatio || 1, 2);

/* ------------------------------------------------------------- skins ----- */

const skins = new Map();
function skin(src) {
  if (!skins.has(src)) {
    skins.set(src, new Promise((ok, no) => {
      const img = new Image();
      img.onload = () => ok(img);
      img.onerror = () => no(new Error(src));
      img.src = src;
    }));
  }
  return skins.get(src);
}

/*
 * The six boxes of a 64x64 player skin, front view, in sprite pixels (16 wide,
 * 32 tall): where each lands, and where its base and overlay faces sit in the
 * texture. The model's right arm is on the viewer's left.
 */
const PARTS = [
  //  dx  dy  w   h   base(u,v)  overlay(u,v)
  [4, 0, 8, 8, 8, 8, 40, 8],      // head + hat
  [4, 8, 8, 12, 20, 20, 20, 36],  // body + jacket
  [0, 8, 4, 12, 44, 20, 44, 36],  // right arm + sleeve
  [12, 8, 4, 12, 36, 52, 52, 52], // left arm + sleeve
  [4, 20, 4, 12, 4, 20, 4, 36],   // right leg + trousers
  [8, 20, 4, 12, 20, 52, 4, 52],  // left leg + trousers
];

/** A composed sprite: base + overlay, optionally darkened, at `s` px per skin pixel. */
function compose(img, s, dark = 0, hurt = 0) {
  const c = document.createElement('canvas');
  c.width = 16 * s; c.height = 32 * s;
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = false;
  for (const [dx, dy, w, h, bu, bv, ou, ov] of PARTS) {
    g.drawImage(img, bu, bv, w, h, dx * s, dy * s, w * s, h * s);
    g.drawImage(img, ou, ov, w, h, dx * s, dy * s, w * s, h * s);
  }
  if (dark > 0) {
    g.globalCompositeOperation = 'source-atop';
    g.fillStyle = `rgba(5,5,9,${(0.9 * dark).toFixed(3)})`;
    g.fillRect(0, 0, c.width, c.height);
    g.globalCompositeOperation = 'source-over';
  }
  if (hurt > 0) {
    g.globalCompositeOperation = 'source-atop';
    g.fillStyle = `rgba(255,40,40,${(0.55 * hurt).toFixed(3)})`;
    g.fillRect(0, 0, c.width, c.height);
    g.globalCompositeOperation = 'source-over';
  }
  return c;
}

/** The eyes layer's face, at `s` px per pixel, on its own canvas so it can glow. */
function composeEyes(img, s) {
  const c = document.createElement('canvas');
  c.width = 8 * s; c.height = 8 * s;
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = false;
  g.drawImage(img, 8, 8, 8, 8, 0, 0, 8 * s, 8 * s);
  return c;
}

/** Draws a sprite with its eyes glowing at (x, y) top-left; sprite is 16s x 32s. */
function drawFigure(g, sprite, eyes, x, y, s, glow = 1) {
  g.drawImage(sprite, x, y, sprite.width, sprite.height);
  if (!eyes) return;
  g.save();
  g.shadowColor = `rgba(255,255,255,${(0.85 * glow).toFixed(2)})`;
  g.shadowBlur = 10 * s * glow;
  g.drawImage(eyes, x + 4 * s, y, 8 * s, 8 * s);
  g.shadowBlur = 0;
  g.drawImage(eyes, x + 4 * s, y, 8 * s, 8 * s);
  g.restore();
}

function fit(canvas, cssW, cssH) {
  canvas.width = Math.round(cssW * DPR);
  canvas.height = Math.round(cssH * DPR);
  canvas.style.width = cssW + 'px';
  canvas.style.height = cssH + 'px';
}

/* ------------------------------------------------------- static cast ----- */

/*
 * A villager's boxes, front view, for the turned: the head is eight by ten with
 * the nose hanging off it, the arms are crossed, the robe overlay runs down over
 * the legs. Texture coordinates are in 64-pixel units and scaled to the file.
 */
function composeVillager(img, s) {
  const k = img.width / 64;
  const c = document.createElement('canvas');
  c.width = 16 * s; c.height = 34 * s;
  const g = c.getContext('2d');
  g.imageSmoothingEnabled = false;
  const box = (u, v, w, h, dx, dy) => g.drawImage(img, u * k, v * k, w * k, h * k, dx * s, dy * s, w * s, h * s);
  box(8, 8, 8, 10, 4, 0);        // head
  box(4, 26, 4, 12, 4, 22);      // right leg
  box(4, 26, 4, 12, 8, 22);      // left leg (same face, mirrored texture in the model)
  box(22, 26, 8, 12, 4, 10);     // body
  box(6, 44, 8, 20, 4, 10);      // robe, over body and legs
  box(48, 26, 4, 8, 0, 8);       // right upper arm
  box(48, 26, 4, 8, 12, 8);      // left upper arm
  box(44, 42, 8, 4, 4, 12);      // crossed forearms
  box(26, 2, 2, 4, 7, 9);        // the nose, last, so it sits over the lip of the robe
  return c;
}

document.querySelectorAll('canvas.sprite').forEach(async (c) => {
  try {
    const img = await skin(c.dataset.skin);
    const eyes = c.dataset.eyes ? composeEyes(await skin(c.dataset.eyes), 4 * DPR) : null;
    fit(c, 72, 144);
    const g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    const s = 4.5 * DPR;
    if (c.dataset.kind === 'villager') {
      const sp = composeVillager(img, s * 32 / 34);
      g.drawImage(sp, (c.width - sp.width) / 2, c.height - sp.height);
    } else {
      drawFigure(g, compose(img, s), eyes, 0, 0, s, 0.8);
    }
  } catch { /* the caption still says who it is */ }
});



/* ------------------------------------------------------------ the chase -- */
/*
 * THE LOOP ON THE LANDING PAGE, and there is no game framework under it.
 *
 * A framework here would be a 400 KB download to move about twenty things, on a
 * page whose whole promise is that it opens instantly on a phone in a field. So
 * this is a small renderer written for exactly this scene: the skins are the
 * mod's own, cut into head/body/arm/leg the way the model is, and swung about
 * their joints — a running man is a hip and a shoulder and a sine wave.
 *
 * Everything behind them is drawn once into offscreen canvases and scrolled at
 * four speeds: mountains almost still, far wood slow, near wood fast, ground
 * fastest. The sky is one gradient interpolated round a forty-second day, with
 * the sun and moon on the same arc and the stars fading in behind them. Nothing
 * is computed twice per frame that can be computed once at load.
 */
(async function chase() {
  const c = document.getElementById('chase');
  if (!c) return;
  let base, angry, addex;
  try {
    [base, angry, addex] = await Promise.all([
      skin('/assets/herobrine.png'), skin('/assets/herobrine_angry.png'), skin('/assets/addexio.png')]);
  } catch { return; }

  const g = c.getContext('2d');

  /* ---- cutting a skin into the pieces of a person, seen from the side ---- */
  /* Face boxes in the 64x64 layout: [u, v, w, h] of the side face, and of the
     overlay above it. Side faces are four pixels wide; the head is eight. */
  const FACES = {
    head: [[0, 8, 8, 8], [32, 8, 8, 8]],
    body: [[16, 20, 4, 12], [16, 36, 4, 12]],
    arm: [[40, 20, 4, 12], [40, 36, 4, 12]],
    leg: [[0, 20, 4, 12], [0, 36, 4, 12]],
  };

  /** One piece, at `s` pixels per skin pixel, optionally darkened or tinted. */
  function piece(img, face, s, dark, warm) {
    const [[u, v, w, h], [ou, ov]] = face;
    const cv = document.createElement('canvas');
    cv.width = Math.max(1, Math.round(w * s));
    cv.height = Math.max(1, Math.round(h * s));
    const x = cv.getContext('2d');
    x.imageSmoothingEnabled = false;
    x.drawImage(img, u, v, w, h, 0, 0, cv.width, cv.height);
    x.drawImage(img, ou, ov, w, h, 0, 0, cv.width, cv.height);
    if (warm) {
      // The player wears the same Steve base as him. A warm shirt is what tells
      // the two of them apart at forty pixels tall.
      const d = x.getImageData(0, 0, cv.width, cv.height);
      const p = d.data;
      for (let i = 0; i < p.length; i += 4) {
        if (p[i + 3] > 0 && p[i + 2] > p[i] + 20 && p[i + 1] > p[i] + 10) {
          const t = p[i + 1];
          p[i] = Math.min(255, t + 40); p[i + 1] = Math.round(t * 0.42); p[i + 2] = Math.round(t * 0.30);
        }
      }
      x.putImageData(d, 0, 0);
    }
    if (dark > 0) {
      x.globalCompositeOperation = 'source-atop';
      x.fillStyle = `rgba(6,6,10,${dark})`;
      x.fillRect(0, 0, cv.width, cv.height);
      x.globalCompositeOperation = 'source-over';
    }
    return cv;
  }

  function figure(img, s, dark, warm) {
    return {
      s,
      head: piece(img, FACES.head, s, dark, false),
      body: piece(img, FACES.body, s, dark, warm),
      arm: piece(img, FACES.arm, s, dark, warm),
      leg: piece(img, FACES.leg, s, dark, warm),
    };
  }

  /**
   * Draws a runner. (x, y) is the point between the feet; `phase` is where the
   * stride is; `face` is -1 running left, 1 running right. Limbs rotate about
   * the joint, which is the top edge of the piece, because that is what a hip
   * and a shoulder do.
   */
  function runner(f, x, y, phase, face, lean, glow) {
    const s = f.s;
    const bob = Math.abs(Math.sin(phase)) * s * 0.6;
    const hip = y - 12 * s - bob;
    const shoulder = hip - 11 * s;
    const swing = Math.sin(phase);
    const swing2 = Math.sin(phase + Math.PI);

    g.save();
    g.translate(Math.round(x), Math.round(y));
    g.scale(face, 1);
    g.rotate(lean);
    g.translate(-Math.round(x), -Math.round(y));

    const limb = (tex, jx, jy, angle) => {
      g.save();
      g.translate(jx, jy);
      g.rotate(angle);
      g.drawImage(tex, -tex.width / 2, 0);
      g.restore();
    };
    // far side first, then the body, then the near side: a person, in order.
    limb(f.leg, x + s * 0.6, hip, swing2 * 0.75);
    limb(f.arm, x - s * 0.4, shoulder, swing * 0.85 + 0.25);
    g.drawImage(f.body, Math.round(x - f.body.width / 2), Math.round(hip - f.body.height));
    const hx = Math.round(x - f.head.width / 2 + s * 0.5);
    const hy = Math.round(shoulder - f.head.height + s * 0.4);
    g.drawImage(f.head, hx, hy);
    if (glow) {
      // Drawn here, inside the flip, so the eyes are on the face and not behind
      // the skull. Everything is drawn facing right and then mirrored.
      g.save();
      g.shadowColor = 'rgba(255,255,255,.95)';
      g.shadowBlur = s * 3.5;
      g.fillStyle = '#fff';
      g.fillRect(hx + f.head.width - s * 2.6, hy + f.head.height * 0.40, s * 2.0, s * 0.9);
      g.restore();
    }
    limb(f.leg, x - s * 0.6, hip, swing * 0.75);
    limb(f.arm, x + s * 0.4, shoulder, swing2 * 0.85 + 0.25);
    g.restore();
    return { shoulder, head: shoulder - f.head.height + s * 0.4 };
  }

  /* ------------------------------------------------------------- the sky -- */
  /* Four keys round the clock. Everything else is a mix of two of them. */
  const SKY = [
    { at: 0.00, top: [96, 150, 214], low: [190, 214, 236], sun: 1.0 },   // day
    { at: 0.34, top: [58, 60, 104], low: [226, 122, 74], sun: 0.35 },    // dusk
    { at: 0.50, top: [8, 10, 22], low: [22, 26, 44], sun: 0.0 },         // night
    { at: 0.84, top: [50, 56, 96], low: [198, 138, 110], sun: 0.35 },    // dawn
  ];
  const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));
  function sky(t) {
    let i = 0;
    for (let k = 0; k < SKY.length; k++) if (t >= SKY[k].at) i = k;
    const a = SKY[i], b = SKY[(i + 1) % SKY.length];
    const span = (b.at > a.at ? b.at : b.at + 1) - a.at;
    const f = Math.min(1, Math.max(0, (t - a.at) / span));
    return { top: mix(a.top, b.top, f), low: mix(a.low, b.low, f), sun: a.sun + (b.sun - a.sun) * f };
  }

  /* ------------------------------------------------------- the scenery ---- */
  let W = 0, H = 0, ground = 0, unit = 3;
  let mountains, farWood, nearWood, floor, stars, figures;
  const rand = (seed) => { let x = seed; return () => (x = (x * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff; };

  function ridge(w, h, base, jag, colour, seed, snow) {
    const cv = document.createElement('canvas');
    cv.width = w; cv.height = h;
    const x = cv.getContext('2d');
    const r = rand(seed);
    const pts = [];
    for (let i = 0; i <= 16; i++) pts.push(base - r() * jag);
    x.beginPath();
    x.moveTo(0, h);
    for (let i = 0; i <= 16; i++) {
      const px = (i / 16) * w;
      x.lineTo(px, pts[i]);
      if (i < 16) x.lineTo(px + w / 32, (pts[i] + pts[i + 1]) / 2 - r() * jag * 0.25);
    }
    x.lineTo(w, h); x.closePath();
    x.fillStyle = colour; x.fill();
    if (snow) {
      // Light down the near flank rather than caps on the peaks: a cap is a
      // rectangle clipped to a jagged edge, and it reads as a box in the sky.
      x.globalCompositeOperation = 'source-atop';
      const lit = x.createLinearGradient(0, base - jag, 0, base + jag * 0.4);
      lit.addColorStop(0, 'rgba(214,224,240,.30)');
      lit.addColorStop(1, 'rgba(214,224,240,0)');
      x.fillStyle = lit; x.fillRect(0, 0, w, h);
      x.globalCompositeOperation = 'source-over';
    }
    return cv;
  }

  function wood(w, h, count, minH, maxH, colour, seed) {
    const cv = document.createElement('canvas');
    cv.width = w; cv.height = h;
    const x = cv.getContext('2d');
    const r = rand(seed);
    x.fillStyle = colour;
    for (let i = 0; i < count; i++) {
      const px = 20 + r() * (w - 40);
      const th = minH + r() * (maxH - minH);
      const tw = th * (0.15 + r() * 0.07);
      x.fillRect(px - tw * 0.09, h - th * 0.34, tw * 0.18, th * 0.34);   // the trunk
      for (let k = 0; k < 4; k++) {                                       // the pine, in steps
        const ky = h - th + (th * 0.72 * k) / 4;
        const kw = tw * (0.45 + (k * 0.55) / 3);
        x.beginPath();
        x.moveTo(px, ky - th * 0.1);
        x.lineTo(px - kw, ky + th * 0.2);
        x.lineTo(px + kw, ky + th * 0.2);
        x.closePath(); x.fill();
      }
    }
    return cv;
  }

  function size() {
    // MEASURED, NEVER PINNED. fit() writes an inline width, and an inline width
    // becomes what clientWidth reports — so a canvas first sized in a narrow
    // window stays narrow for ever, in a band that is supposed to be full width.
    // Here the CSS owns the size and only the backing store is set from it.
    const cssW = Math.max(240, c.clientWidth || c.parentElement.clientWidth || 900);
    const cssH = Math.max(120, c.clientHeight || 240);
    c.width = Math.round(cssW * DPR);
    c.height = Math.round(cssH * DPR);
    g.imageSmoothingEnabled = false;
    W = c.width; H = c.height;
    ground = Math.round(H * 0.86);
    unit = Math.max(2, H / 108);                      // pixels per skin pixel for the runners
    mountains = [
      ridge(W, ground, ground * 0.52, ground * 0.30, '#2c3550', 7, true),
      ridge(W, ground, ground * 0.66, ground * 0.22, '#232a41', 23, false),
    ];
    farWood = wood(W, ground, Math.round(W / 30), ground * 0.16, ground * 0.32, '#151d24', 41);
    nearWood = wood(W, ground + H * 0.1, Math.max(2, Math.round(W / 420)), H * 0.55, H * 0.85, '#070a0d', 59);
    floor = (() => {
      const cv = document.createElement('canvas');
      cv.width = W; cv.height = H - ground;
      const x = cv.getContext('2d');
      x.fillStyle = '#10161a'; x.fillRect(0, 0, W, cv.height);
      const r = rand(97);
      for (let i = 0; i < W / 5; i++) {
        x.fillStyle = r() > 0.5 ? '#141a1e' : '#0b1013';
        x.fillRect(r() * W, r() * cv.height, 2 + r() * 4, 1 + r() * 2);
      }
      return cv;
    })();
    stars = Array.from({ length: Math.round(W / 26) }, (_, i) => {
      const r = rand(i * 31 + 3);
      return { x: r() * W, y: r() * ground * 0.7, a: 0.3 + r() * 0.7 };
    });
    figures = {
      you: figure(base, unit, 0.05, true),
      addexio: figure(addex, unit, 0.05, false),
      him: figure(angry, unit * 1.6, 0.74, false),
    };
  }

  /* ------------------------------------------------------------- the fire - */
  const fires = [];
  const bursts = [];
  const embers = [];

  function throwFire(fromX, fromY, toX, toY) {
    const dx = toX - fromX, dy = toY - fromY;
    const time = 46;
    fires.push({ x: fromX, y: fromY, vx: dx / time, vy: dy / time - 0.13 * time * 0.5 / time * 6, life: time + 20, g: 0.035 * (H / 300) });
  }

  /* ----------------------------------------------------------- the frame -- */
  let visible = true, last = 0, t0 = 0, resizeTimer = 0, fireIn = 90;
  size();
  // A ResizeObserver, not a resize listener: the element's box can go from zero
  // to real without the window ever resizing — a pane that opens, a font that
  // lands, a phone rotating — and a scene built at zero stays wrong for ever.
  if ('ResizeObserver' in window) {
    new ResizeObserver(() => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => {
        if (c.clientWidth > 80 && Math.abs(c.width / DPR - c.clientWidth) > 2) size();
      }, 120);
    }).observe(c);
  } else {
    addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(size, 160); }, { passive: true });
  }
  new IntersectionObserver((es) => {
    visible = es[0].isIntersecting;
    if (visible) { if (c.clientWidth > 80 && Math.abs(c.width / DPR - c.clientWidth) > 2) size(); requestAnimationFrame(frame); }
  }).observe(c);

  const CYCLE = 46000;         // a day and a night, in ms
  function frame(now) {
    if (!visible) return;
    if (now - last < 32) { requestAnimationFrame(frame); return; }       // 30 fps
    if (!t0) t0 = now;
    last = now;
    const t = ((now - t0) % CYCLE) / CYCLE;
    const s = sky(t);
    const night = 1 - Math.min(1, s.sun / 0.9);
    const run = now / 1000;
    const scroll = (now / 1000) * (H * 0.55);

    // sky
    const grad = g.createLinearGradient(0, 0, 0, ground);
    grad.addColorStop(0, `rgb(${s.top.join(',')})`);
    grad.addColorStop(1, `rgb(${s.low.join(',')})`);
    g.fillStyle = grad; g.fillRect(0, 0, W, ground);

    // stars, then the sun and the moon on one arc
    if (night > 0.02) {
      for (const st of stars) {
        g.fillStyle = `rgba(226,232,240,${(st.a * night * 0.9).toFixed(2)})`;
        g.fillRect(st.x, st.y, DPR, DPR);
      }
    }
    const arc = (t * Math.PI * 2) - Math.PI / 2;
    const cx = W / 2 - Math.cos(arc) * W * 0.40;
    const cy = ground * 0.72 - Math.sin(arc) * ground * 0.62;
    const disc = Math.max(6, H * 0.045);
    g.save();
    g.shadowColor = s.sun > 0.5 ? 'rgba(255,226,170,.75)' : 'rgba(200,215,240,.5)';
    g.shadowBlur = disc * 1.6;
    g.fillStyle = s.sun > 0.5 ? '#ffe9b0' : '#dfe6f2';
    g.fillRect(cx - disc / 2, cy - disc / 2, disc, disc);
    g.restore();

    // the country, at four speeds
    const layer = (img, speed, y) => {
      const off = (scroll * speed) % W;
      g.drawImage(img, -off, y);
      g.drawImage(img, W - off, y);
    };
    layer(mountains[0], 0.04, 0);
    layer(mountains[1], 0.09, 0);
    layer(farWood, 0.34, 0);
    // a low band of light where the sky meets the wood, so black shapes read
    const horizon = g.createLinearGradient(0, ground - H * 0.22, 0, ground);
    horizon.addColorStop(0, 'rgba(255,255,255,0)');
    horizon.addColorStop(1, `rgba(${s.low.join(',')},${(0.45 - night * 0.28).toFixed(2)})`);
    g.fillStyle = horizon; g.fillRect(0, ground - H * 0.22, W, H * 0.22);
    g.drawImage(floor, 0, ground);
    layer(floor, 1.25, ground);

    // the three of them, and the dust off their heels
    const stride = run * 9;
    const shadow = (x, w2) => {
      g.fillStyle = 'rgba(0,0,0,.45)';
      g.beginPath(); g.ellipse(x, ground + unit * 1.4, w2, unit * 0.7, 0, 0, Math.PI * 2); g.fill();
    };
    shadow(W * 0.24, unit * 3); shadow(W * 0.40, unit * 3);
    const you = runner(figures.you, W * 0.24, ground + unit * 1.2, stride, -1, -0.07);
    const add = runner(figures.addexio, W * 0.40, ground + unit * 1.2, stride + 1.9, -1, -0.06);
    layer(nearWood, 1.0, ground - (ground + H * 0.1) + H * 0.1);
    shadow(W * 0.78, unit * 4.6);
    const him = runner(figures.him, W * 0.78, ground + unit * 2.0, stride * 0.86 + 0.7, -1, -0.10, true);

    if (Math.random() < 0.7) {
      embers.push({ x: W * 0.78 + (Math.random() - 0.5) * unit * 6, y: him.shoulder + Math.random() * unit * 10,
        r: unit * (0.7 + Math.random()), a: 0.34, vy: -unit * 0.06, vx: unit * (0.05 + Math.random() * 0.06), smoke: true });
    }

    // fire: he throws at where they are going, and it bursts on the ground
    if (--fireIn <= 0) {
      fireIn = 80 + Math.round(Math.random() * 70);
      throwFire(W * 0.78 - figures.him.body.width, him.shoulder + unit * 1.5,
        W * 0.24 + (Math.random() - 0.2) * W * 0.08, ground - unit * 2);
    }
    for (let i = fires.length - 1; i >= 0; i--) {
      const f = fires[i];
      f.x += f.vx; f.y += f.vy; f.vy += f.g; f.life--;
      embers.push({ x: f.x + (Math.random() - 0.5) * unit * 1.6, y: f.y + (Math.random() - 0.5) * unit * 1.6,
        r: unit * (0.5 + Math.random() * 0.8), a: 0.6, vy: -unit * 0.04, vx: unit * 0.1, smoke: false });
      g.save();
      g.shadowColor = 'rgba(255,120,20,.95)'; g.shadowBlur = unit * 7;
      g.fillStyle = '#ff8a1e';
      g.fillRect(f.x - unit * 1.3, f.y - unit * 1.3, unit * 2.6, unit * 2.6);
      g.fillStyle = '#fff2c8';
      g.fillRect(f.x - unit * 0.5, f.y - unit * 0.5, unit * 1.1, unit * 1.1);
      g.restore();
      if (f.y >= ground || f.life <= 0 || f.x < -20) {
        bursts.push({ x: f.x, y: Math.min(f.y, ground), r: unit * 2, life: 14 });
        fires.splice(i, 1);
      }
    }
    for (let i = bursts.length - 1; i >= 0; i--) {
      const b = bursts[i];
      const k = 1 - b.life / 14;
      const rr = b.r + k * unit * 11;
      const flare = g.createRadialGradient(b.x, b.y, 0, b.x, b.y, rr);
      flare.addColorStop(0, `rgba(255,246,214,${(0.9 * (1 - k)).toFixed(2)})`);
      flare.addColorStop(0.45, `rgba(255,138,30,${(0.6 * (1 - k)).toFixed(2)})`);
      flare.addColorStop(1, 'rgba(255,90,10,0)');
      g.fillStyle = flare;
      g.beginPath(); g.arc(b.x, b.y, rr, 0, Math.PI * 2); g.fill();
      if (b.life === 14) {
        for (let n = 0; n < 14; n++) {
          embers.push({ x: b.x, y: b.y, r: unit * (0.5 + Math.random()), a: 0.9,
            vx: (Math.random() - 0.5) * unit * 1.6, vy: -Math.random() * unit * 1.2, smoke: false });
        }
      }
      if (--b.life <= 0) bursts.splice(i, 1);
    }
    for (let i = embers.length - 1; i >= 0; i--) {
      const e = embers[i];
      g.fillStyle = e.smoke ? `rgba(18,18,24,${e.a.toFixed(2)})` : `rgba(255,${140 + Math.round(Math.random() * 60)},50,${e.a.toFixed(2)})`;
      g.fillRect(e.x, e.y, e.r, e.r);
      e.x += e.vx; e.y += e.vy; e.a -= e.smoke ? 0.008 : 0.055;
      if (e.a <= 0) embers.splice(i, 1);
    }
    if (embers.length > 160) embers.splice(0, embers.length - 160);

    if (night > 0.02) {
      g.fillStyle = `rgba(6,8,18,${(night * 0.42).toFixed(2)})`;
      g.fillRect(0, 0, W, H);
    }
    const vig = g.createLinearGradient(0, 0, 0, H);
    vig.addColorStop(0, 'rgba(15,15,18,.55)');
    vig.addColorStop(0.35, 'rgba(15,15,18,0)');
    vig.addColorStop(1, 'rgba(15,15,18,.75)');
    g.fillStyle = vig; g.fillRect(0, 0, W, H);
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
})();

/* -------------------------------------------------------- the arena ------ */
/*
 * The fight, in miniature and honestly: a hundred blows, acts at thirds, blow
 * spacing that widens each act, the rise between acts, the dark at three, and
 * the ending. Tap him. It is a toy, but it is the same rules as the mod.
 */
(async function arena() {
  const c = document.getElementById('stage');
  if (!c) return;
  const fill = document.getElementById('bossfill');
  const blowsEl = document.getElementById('blows');
  const nameEl = document.getElementById('bossname');
  const hint = document.getElementById('hint');
  const buttons = [...document.querySelectorAll('.lvl')];

  let base, angry, eyesImg;
  try { [base, angry, eyesImg] = await Promise.all([skin('/assets/herobrine.png'), skin('/assets/herobrine_angry.png'), skin('/assets/herobrine_eyes.png')]); }
  catch { hint.textContent = 'The skins did not load — but he is still out there.'; return; }

  const TOTAL = 100;
  const ACT_AT = [0, 34, 67];                 // blows at which each act begins
  const SCALE = [1.0, 1.4, 1.7];
  const SPACING = [250, 325, 400];            // ms between counted blows, per act
  const NAMES = ['Herobrine', 'Herobrine · II', 'Herobrine · III'];

  const g = c.getContext('2d');
  const cache = new Map();
  let unit = 2, ground = 0;
  function size() {
    const room = c.parentElement.clientWidth;
    const cssW = room > 120 ? Math.min(room - 36, 420) : 320, cssH = cssW * 1.25;
    fit(c, cssW, cssH);
    g.imageSmoothingEnabled = false;
    unit = Math.max(2, Math.floor((cssH * DPR) / 62));   // px per skin pixel at level 1
    ground = c.height - 6 * unit;
    cache.clear();
    eyesFor = [1, 2, 3].map((l) => composeEyes(eyesImg, Math.round(unit * SCALE[l - 1])));
  }
  let eyesFor = [];
  function spriteFor(level, dark, hurt) {
    const key = `${level}|${dark.toFixed(2)}|${hurt.toFixed(2)}`;
    if (!cache.has(key)) {
      const img = level >= 2 ? angry : base;
      cache.set(key, compose(img, Math.round(unit * SCALE[level - 1]), dark, hurt));
    }
    return cache.get(key);
  }
  const state = { level: 1, blows: 0, lastBlow: 0, hurtUntil: 0, shake: 0, over: false,
    ceremony: null, flash: 0, smoke: [], sparks: [] };

  function setBar() {
    const left = Math.max(0, 1 - state.blows / TOTAL);
    fill.style.width = (left * 100).toFixed(1) + '%';
    blowsEl.textContent = `${state.blows} / ${TOTAL} blows`;
    nameEl.textContent = state.over ? 'Removed Herobrine.' : NAMES[state.level - 1];
    buttons.forEach((b) => b.classList.toggle('is-on', Number(b.dataset.level) === state.level));
  }

  function say(text) { hint.textContent = text; }

  /** The rise between acts: up, dark, bolts, hang, drop, slam. */
  function beginCeremony(toLevel, ending = false) {
    state.ceremony = { start: performance.now(), toLevel, ending, from: state.level };
    say(ending ? 'The last blow.' : `Act ${toLevel}. He rises.`);
  }

  function strike(e) {
    if (state.over) { reset(); return; }
    if (state.ceremony) return;
    const now = performance.now();
    const act = state.level;
    // Where did they tap? Only a tap on him lands.
    const r = c.getBoundingClientRect();
    const px = (e.clientX - r.left) * (c.width / r.width), py = (e.clientY - r.top) * (c.height / r.height);
    const sp = spriteFor(act, act === 3 ? 1 : 0, 0);
    const x = (c.width - sp.width) / 2, y = ground - sp.height;
    if (px < x - 2 * unit || px > x + sp.width + 2 * unit || py < y - 2 * unit || py > ground) { say('Closer. He is not there.'); return; }

    if (now - state.lastBlow < SPACING[act - 1]) {
      say(act === 1 ? 'Connected — not counted. Too fast.' : 'His skin is thicker now. Slower.');
      state.hurtUntil = now + 90; state.shake = 2;
      return;
    }
    state.lastBlow = now;
    state.blows++;
    state.hurtUntil = now + 140; state.shake = 5;
    setBar();
    if (state.blows >= TOTAL) { beginCeremony(3, true); return; }
    if (state.blows >= ACT_AT[2] && state.level < 3) { beginCeremony(3); return; }
    if (state.blows >= ACT_AT[1] && state.level < 2) { beginCeremony(2); return; }
    const left = [ACT_AT[1], ACT_AT[2], TOTAL][act - 1] - state.blows;
    say(left <= 3 ? 'Almost. Keep your rhythm.' : `${left} to ${act === 3 ? 'the end' : 'the next act'}.`);
  }

  function reset() {
    Object.assign(state, { level: 1, blows: 0, over: false, ceremony: null, flash: 0, smoke: [], sparks: [] });
    setBar(); say('Tap or click him to strike.');
  }

  c.addEventListener('pointerdown', (e) => { e.preventDefault(); strike(e); });
  buttons.forEach((b) => b.addEventListener('click', () => {
    if (state.over) reset();
    const to = Number(b.dataset.level);
    if (to === state.level || state.ceremony) return;
    state.blows = ACT_AT[to - 1];
    setBar();
    if (to > state.level) beginCeremony(to);
    else { state.level = to; setBar(); say(`Back to act ${to}.`); }
  }));

  function puff(sp, x, y) {
    return { x: x + Math.random() * sp.width, y: y + sp.height * (0.15 + Math.random() * 0.75),
      r: (1 + Math.random() * 1.6) * unit, a: 0.3 + Math.random() * 0.25, vy: (0.12 + Math.random() * 0.25) * unit, vx: (Math.random() - 0.5) * 0.25 * unit };
  }

  let visible = true, last = 0, resizeTimer = 0;
  size();
  addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(size, 150); }, { passive: true });
  new IntersectionObserver((es) => { visible = es[0].isIntersecting; if (visible) { if (c.parentElement.clientWidth > 120 && Math.abs(c.width / DPR - Math.min(c.parentElement.clientWidth - 36, 420)) > 2) size(); requestAnimationFrame(frame); } }).observe(c);

  function frame(now) {
    if (!visible) return;
    if (now - last < 25) { requestAnimationFrame(frame); return; }     // ~40 fps
    last = now;
    const t = now / 1000;
    g.clearRect(0, 0, c.width, c.height);

    // The hall: a dark floor line and a faint back wall of deepslate.
    g.fillStyle = '#0b0b0f'; g.fillRect(0, 0, c.width, c.height);
    for (let i = 0; i < 6; i++) {
      g.fillStyle = i % 2 ? '#111116' : '#0e0e13';
      g.fillRect(0, ground - (i + 1) * 9 * unit, c.width, 9 * unit);
    }
    g.fillStyle = '#1c1c22'; g.fillRect(0, ground, c.width, c.height - ground);
    g.fillStyle = '#24242b'; g.fillRect(0, ground, c.width, unit);

    let level = state.level, dark = level === 3 ? 1 : 0, hurt = now < state.hurtUntil ? 1 : 0;
    let lift = 0, white = 0, scaleLevel = level, glow = 0.75 + 0.25 * Math.sin(t * 2.4);

    const cer = state.ceremony;
    if (cer) {
      const e = (now - cer.start) / 1000;
      const RISE = 1.3, HANG = 0.6, DROP = 0.35;
      if (cer.ending) {
        lift = Math.min(1, e / 2.2) * 14 * unit;
        white = Math.min(1, e / 2.4);
        dark = 0; hurt = 0;
        if (e > 2.6) { state.ceremony = null; state.over = true; setBar(); say('The rain stops. Somewhere behind you, it is getting light. Tap to play again.'); }
      } else if (e < RISE) {
        lift = (e / RISE) * 14 * unit; dark = Math.max(dark, e / RISE);
        if (!REDUCED && Math.random() < 0.08) state.sparks.push({ x: Math.random() * c.width, life: 6 });
      } else if (e < RISE + HANG) {
        lift = 14 * unit; dark = 1;
      } else if (e < RISE + HANG + DROP) {
        const d = (e - RISE - HANG) / DROP;
        lift = 14 * unit * (1 - d * d); dark = cer.toLevel === 3 ? 1 : 1 - d;
        scaleLevel = cer.toLevel;
      } else {
        state.level = cer.toLevel; state.ceremony = null; state.shake = 8; setBar();
        say(cer.toLevel === 3 ? 'The light does not come back.' : 'Bigger. Slower blows now, or they will not count.');
        for (let i = 0; i < 30; i++) state.smoke.push({ x: c.width / 2 + (Math.random() - 0.5) * 24 * unit, y: ground - Math.random() * 3 * unit, r: 2 * unit, a: 0.5, vy: (0.2 + Math.random() * 0.4) * unit, vx: (Math.random() - 0.5) * 1.2 * unit });
        level = state.level; scaleLevel = level; dark = level === 3 ? 1 : 0;
      }
    }

    const sp = spriteFor(scaleLevel, Math.min(1, dark), hurt);
    const eyes = eyesFor[scaleLevel - 1];
    const sx = Math.round((c.width - sp.width) / 2 + (state.shake > 0 ? (Math.random() - 0.5) * state.shake * unit * 0.6 : 0));
    const sy = Math.round(ground - sp.height - lift + (REDUCED || cer ? 0 : Math.sin(t * 1.4) * unit * 0.3));
    if (state.shake > 0) state.shake -= 0.4;

    // The dark comes off him at three, and while he rises.
    if (!REDUCED && (dark >= 0.5) && !state.over && Math.random() < 0.6) state.smoke.push(puff(sp, sx, sy));
    for (let i = state.smoke.length - 1; i >= 0; i--) {
      const p = state.smoke[i];
      g.fillStyle = `rgba(16,16,22,${p.a.toFixed(2)})`;
      g.fillRect(Math.round(p.x), Math.round(p.y), p.r, p.r);
      p.y -= p.vy; p.x += p.vx; p.a -= 0.006;
      if (p.a <= 0) state.smoke.splice(i, 1);
    }
    if (state.smoke.length > 90) state.smoke.splice(0, state.smoke.length - 90);

    if (!state.over) {
      drawFigure(g, sp, eyes, sx, sy, Math.round(unit * SCALE[scaleLevel - 1]), glow);
      if (white > 0) {
        g.save(); g.globalAlpha = white; g.globalCompositeOperation = 'lighter';
        const wsp = spriteFor(scaleLevel, 0, 0);
        g.drawImage(wsp, sx, sy); g.fillStyle = `rgba(255,255,255,${(white * 0.9).toFixed(2)})`;
        g.globalCompositeOperation = 'source-over';
        g.restore();
        g.save(); g.globalAlpha = white * 0.9; g.fillStyle = '#fff';
        g.globalCompositeOperation = 'source-atop'; g.restore();
        // a plain white veil over him, growing
        g.save(); g.beginPath(); g.rect(sx, sy, sp.width, sp.height); g.clip();
        g.globalCompositeOperation = 'lighter'; g.globalAlpha = white;
        g.drawImage(sp, sx, sy); g.drawImage(sp, sx, sy); g.restore();
      }
    } else {
      // After: a sunset behind the empty floor.
      const grad = g.createLinearGradient(0, 0, 0, ground);
      grad.addColorStop(0, '#2a2140'); grad.addColorStop(0.55, '#7a3a3a'); grad.addColorStop(1, '#e0a060');
      g.fillStyle = grad; g.fillRect(0, 0, c.width, ground);
      g.fillStyle = '#1c1c22'; g.fillRect(0, ground, c.width, c.height - ground);
    }

    // Bolts: white verticals for a few frames, and a flash on the floor.
    for (let i = state.sparks.length - 1; i >= 0; i--) {
      const b = state.sparks[i];
      g.fillStyle = `rgba(255,255,255,${(b.life / 6).toFixed(2)})`;
      g.fillRect(Math.round(b.x), 0, unit, ground);
      g.fillRect(Math.round(b.x) - 3 * unit, ground - unit, 7 * unit, unit);
      if (--b.life <= 0) state.sparks.splice(i, 1);
    }
    requestAnimationFrame(frame);
  }
  setBar();
  requestAnimationFrame(frame);
})();

/* -------------------------------------------------------------- rain ----- */
(function rain() {
  const canvas = document.getElementById('rain');
  if (!canvas || REDUCED) return;
  const ctx = canvas.getContext('2d', { alpha: true });
  let w, h, drops;
  function size() {
    w = canvas.width = innerWidth * DPR; h = canvas.height = innerHeight * DPR;
    canvas.style.width = innerWidth + 'px'; canvas.style.height = innerHeight + 'px';
    const count = Math.round((innerWidth * innerHeight) / 22000);
    drops = Array.from({ length: count }, () => spawn(true));
  }
  function spawn(anywhere) {
    return { x: Math.random() * w, y: anywhere ? Math.random() * h : -20 * DPR, v: (1.4 + Math.random() * 3) * DPR, len: (4 + Math.random() * 8) * DPR, a: 0.05 + Math.random() * 0.18 };
  }
  let last = 0;
  function frame(now) {
    if (now - last < 33) { requestAnimationFrame(frame); return; }
    last = now;
    ctx.clearRect(0, 0, w, h);
    for (const d of drops) {
      ctx.fillStyle = `rgba(190,205,230,${d.a})`;
      ctx.fillRect(Math.round(d.x), Math.round(d.y), Math.max(1, DPR), d.len);
      d.y += d.v;
      if (d.y > h) Object.assign(d, spawn(false));
    }
    requestAnimationFrame(frame);
  }
  addEventListener('resize', size, { passive: true });
  size(); requestAnimationFrame(frame);
})();

/* ------------------------------------------------------------ reveal ----- */
(function reveal() {
  const items = document.querySelectorAll('.reveal');
  if (!('IntersectionObserver' in window)) { items.forEach((n) => n.classList.add('seen')); return; }
  const io = new IntersectionObserver((entries) => {
    entries.forEach((entry) => { if (entry.isIntersecting) { entry.target.classList.add('seen'); io.unobserve(entry.target); } });
  }, { rootMargin: '0px 0px -10% 0px' });
  items.forEach((n) => io.observe(n));
})();

/* --------------------------------------------------------------- nav ----- */
(function nav() {
  const bar = document.querySelector('.nav');
  addEventListener('scroll', () => bar.classList.toggle('stuck', scrollY > 8), { passive: true });
})();

/* -------------------------------------------------------------- copy ----- */
document.querySelectorAll('.copy').forEach((btn) => {
  btn.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(btn.dataset.copy);
      const was = btn.textContent; btn.textContent = 'copied'; btn.classList.add('done');
      setTimeout(() => { btn.textContent = was; btn.classList.remove('done'); }, 1400);
    } catch { btn.textContent = btn.dataset.copy; }
  });
});

/* ---------------------------------------------------- latest release ----- */
/*
 * Asks GitHub what the newest release is so every button says a real version
 * and links at the actual files. Optional: the markup already points at
 * /releases/latest, so nothing here has to succeed.
 */
(function latest() {
  const REPO = 'robinandrejohansen/herobrine-mod';
  fetch(`https://api.github.com/repos/${REPO}/releases/latest`, { headers: { Accept: 'application/vnd.github+json' } })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((rel) => {
      const assets = rel.assets || [];
      const jar = assets.find((a) => a.name.endsWith('.jar'));
      const pack = assets.find((a) => a.name.endsWith('.mrpack'));
      const ver = String(rel.tag_name).replace(/^v/, '');
      const set = (id, href, meta) => {
        const el = document.getElementById(id); if (!el) return;
        if (href) el.href = href;
        const m = el.querySelector('.btn-meta'); if (m && meta) m.textContent = meta;
      };
      if (jar) {
        const mb = (jar.size / 1048576).toFixed(1);
        set('download', jar.browser_download_url, `${rel.tag_name} · ${mb} MB · .jar`);
        set('dl-jar', jar.browser_download_url, `${rel.tag_name} · ${mb} MB`);
      }
      if (pack) set('pack', pack.browser_download_url, `${rel.tag_name} · .mrpack`);
      document.querySelectorAll('.ver').forEach((n) => { n.textContent = ver; });
    })
    .catch(() => { /* the static links are already correct */ });
})();
