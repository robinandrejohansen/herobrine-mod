/*
 * No framework, no build step, no dependencies. The whole site is three files
 * and it stays that way — a page about a mod should not take longer to load
 * than the mod takes to install.
 */

/* ------------------------------------------------------------------ rain -- */
/*
 * Square drops, falling on a grid. Real rain on a canvas would be diagonal
 * streaks with motion blur; this is deliberately the other thing, because the
 * one pixel idea the page commits to is that everything is made of blocks.
 * Sized off devicePixelRatio so it stays crisp on a phone.
 */
(function rain() {
  const canvas = document.getElementById('rain');
  if (!canvas || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const ctx = canvas.getContext('2d', { alpha: true });

  let w, h, dpr, drops;
  function size() {
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    w = canvas.width = innerWidth * dpr;
    h = canvas.height = innerHeight * dpr;
    canvas.style.width = innerWidth + 'px';
    canvas.style.height = innerHeight + 'px';
    // Fewer drops on a small screen: it is atmosphere, not weather.
    const count = Math.round((innerWidth * innerHeight) / 14000);
    drops = Array.from({ length: count }, () => spawn(true));
  }
  function spawn(anywhere) {
    return {
      x: Math.random() * w,
      y: anywhere ? Math.random() * h : -20 * dpr,
      v: (1.6 + Math.random() * 3.4) * dpr,
      len: (4 + Math.random() * 9) * dpr,
      a: 0.06 + Math.random() * 0.22
    };
  }

  function frame() {
    ctx.clearRect(0, 0, w, h);
    for (const d of drops) {
      ctx.fillStyle = `rgba(190,205,230,${d.a})`;
      ctx.fillRect(Math.round(d.x), Math.round(d.y), Math.max(1, dpr), d.len);
      d.y += d.v;
      if (d.y > h) Object.assign(d, spawn(false));
    }
    requestAnimationFrame(frame);
  }

  addEventListener('resize', size, { passive: true });
  size();
  frame();
})();

/* ------------------------------------------------------------------ eyes -- */
/*
 * They drift a few pixels toward the cursor. Not tracking it — drifting, and
 * slowly, so it reads as something noticing you rather than a widget following
 * the mouse. On touch devices there is no cursor and they simply sit still,
 * which is fine.
 */
(function eyes() {
  const el = document.getElementById('eyes');
  if (!el) return;
  addEventListener('pointermove', (e) => {
    const dx = (e.clientX / innerWidth - 0.5) * 16;
    const dy = (e.clientY / innerHeight - 0.5) * 10;
    el.style.setProperty('--ex', dx.toFixed(1) + 'px');
    el.style.setProperty('--ey', dy.toFixed(1) + 'px');
  }, { passive: true });
})();

/* ---------------------------------------------------------------- reveal -- */

(function reveal() {
  const items = document.querySelectorAll('.reveal');
  if (!('IntersectionObserver' in window)) {
    items.forEach((n) => n.classList.add('seen'));
    return;
  }
  const io = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('seen');
      io.unobserve(entry.target);
    });
  }, { rootMargin: '0px 0px -12% 0px' });
  items.forEach((n) => io.observe(n));
})();

/* ---------------------------------------------------------------- ladder -- */
/*
 * The spine fills as you scroll past it and each phase lights when its own tick
 * crosses the middle of the screen. It is the one piece of scroll-linked motion
 * on the page, and it is here because the phases ARE a progression — a static
 * list would say six things where this says one.
 */
(function ladder() {
  const list = document.querySelector('.ladder');
  if (!list) return;
  const phases = [...list.querySelectorAll('.phase')];

  function paint() {
    const box = list.getBoundingClientRect();
    const mid = innerHeight * 0.55;
    const run = (mid - box.top) / box.height;
    list.style.setProperty('--fill', Math.max(0, Math.min(1, run)) * 100 + '%');
    phases.forEach((p) => {
      p.classList.toggle('lit', p.getBoundingClientRect().top < mid);
    });
  }
  addEventListener('scroll', paint, { passive: true });
  addEventListener('resize', paint, { passive: true });
  paint();
})();

/* ------------------------------------------------------------------- nav -- */

(function nav() {
  const bar = document.querySelector('.nav');
  addEventListener('scroll', () => {
    bar.classList.toggle('stuck', scrollY > 8);
  }, { passive: true });
})();

/* ------------------------------------------------------------------ copy -- */

document.querySelectorAll('.copy').forEach((btn) => {
  btn.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(btn.dataset.copy);
      const was = btn.textContent;
      btn.textContent = 'copied';
      btn.classList.add('done');
      setTimeout(() => { btn.textContent = was; btn.classList.remove('done'); }, 1400);
    } catch {
      // Clipboard refused (insecure context, or the user said no). Show the
      // path instead of failing silently — they can still select it by hand.
      btn.textContent = btn.dataset.copy;
    }
  });
});

/* ------------------------------------------------------- latest release -- */
/*
 * Asks GitHub what the newest release is so the button says a real version and
 * links at the actual jar. Entirely optional: if the repo is private, or the
 * request is rate-limited, or the network is gone, the markup already points at
 * /releases/latest and nothing here has to succeed.
 */
(function latest() {
  const REPO = 'robinandrejohansen/herobrine-mod';
  const meta = document.getElementById('dl-meta');
  const link = document.getElementById('download');
  if (!meta || !link) return;

  fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
    headers: { Accept: 'application/vnd.github+json' }
  })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((rel) => {
      const jar = (rel.assets || []).find((a) => a.name.endsWith('.jar'));
      if (jar) {
        link.href = jar.browser_download_url;
        const mb = (jar.size / 1048576).toFixed(1);
        meta.textContent = `${rel.tag_name} · ${mb} MB`;
      } else {
        meta.textContent = rel.tag_name;
      }
      document.querySelectorAll('.ver').forEach((n) => {
        n.textContent = String(rel.tag_name).replace(/^v/, '');
      });
    })
    .catch(() => { /* the static link is already correct */ });
})();
