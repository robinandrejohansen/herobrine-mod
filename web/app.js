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

/* --------------------------------------------------------- the hero ------ */
/*
 * Level three, standing in the dark, breathing. The eyes drift a little toward
 * the cursor — noticing, not tracking.
 */
(async function hero() {
  const c = document.getElementById('hero-sprite');
  if (!c) return;
  let img, eyesImg;
  try { [img, eyesImg] = await Promise.all([skin('/assets/herobrine_angry.png'), skin('/assets/herobrine_eyes.png')]); }
  catch { return; }

  const g = c.getContext('2d');
  let s, sprite, eyes, x, y, smoke = [];
  function size() {
    // Measured when it is actually laid out — a hidden tab reports zero.
    const cssW = c.clientWidth > 40 ? c.clientWidth : 300, cssH = cssW * 5 / 3;
    fit(c, cssW, cssH);
    g.imageSmoothingEnabled = false;
    s = Math.max(2, Math.floor((cssH * DPR) / 34));
    sprite = compose(img, s, 1);
    eyes = composeEyes(eyesImg, s);
    x = (c.width - sprite.width) / 2; y = c.height - sprite.height - 2 * s;
    smoke = Array.from({ length: 18 }, () => puff(true));
  }
  function puff(any) {
    return { x: x + Math.random() * sprite.width, y: any ? y + Math.random() * sprite.height : y + sprite.height * (0.2 + Math.random() * 0.7),
      r: (1 + Math.random() * 2) * s, a: 0.25 + Math.random() * 0.25, vy: (0.15 + Math.random() * 0.25) * s, vx: (Math.random() - 0.5) * 0.3 * s };
  }
  let mx = 0, my = 0;
  addEventListener('pointermove', (e) => { mx = (e.clientX / innerWidth - 0.5) * 2; my = (e.clientY / innerHeight - 0.5) * 2; }, { passive: true });

  let visible = true, resizeTimer = 0;
  size();
  addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(size, 150); }, { passive: true });
  new IntersectionObserver((es) => { visible = es[0].isIntersecting; if (visible) { if (c.clientWidth > 40 && Math.abs(c.width / DPR - c.clientWidth) > 2) size(); requestAnimationFrame(frame); } }).observe(c);
  let last = 0;
  function frame(now) {
    if (!visible) return;                       // the browser itself pauses rAF in a hidden tab
    if (now - last < 33) { requestAnimationFrame(frame); return; }   // 30 fps is plenty for a still man
    last = now;
    const t = now / 1000;
    g.clearRect(0, 0, c.width, c.height);
    if (!REDUCED) {
      for (const p of smoke) {
        g.fillStyle = `rgba(20,20,26,${p.a.toFixed(2)})`;
        g.fillRect(Math.round(p.x), Math.round(p.y), p.r, p.r);
        p.y -= p.vy; p.x += p.vx; p.a -= 0.004;
        if (p.a <= 0 || p.y < y - 10 * s) Object.assign(p, puff(false));
      }
    }
    const bob = REDUCED ? 0 : Math.sin(t * 1.3) * s * 0.35;
    drawFigure(g, sprite, eyes, Math.round(x + mx * s * 0.6), Math.round(y + bob + my * s * 0.3), s, 0.8 + 0.2 * Math.sin(t * 2.2));
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
