/* His face, drawn from his own skin: the front of the head, eight by eight, with
 * the white eyes laid over it. And the download buttons told the real version. */
(function face() {
  const canvas = document.getElementById('face');
  if (!canvas) return;
  const g = canvas.getContext('2d');
  g.imageSmoothingEnabled = false;
  const load = (src) => new Promise((ok, no) => { const i = new Image(); i.onload = () => ok(i); i.onerror = no; i.src = src; });
  Promise.all([load('/assets/herobrine.png'), load('/assets/herobrine_eyes.png').catch(() => null)])
    .then(([skin, eyes]) => {
      g.drawImage(skin, 8, 8, 8, 8, 0, 0, 8, 8);     // head, front
      g.drawImage(skin, 40, 8, 8, 8, 0, 0, 8, 8);    // the hat layer over it
      if (eyes) g.drawImage(eyes, 8, 8, 8, 8, 0, 0, 8, 8);
    })
    .catch(() => {});
})();

(function latest() {
  const REPO = 'robinandrejohansen/herobrine-mod';
  fetch(`https://api.github.com/repos/${REPO}/releases/latest`, { headers: { Accept: 'application/vnd.github+json' } })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((rel) => {
      const assets = rel.assets || [];
      const jar = assets.find((a) => a.name.endsWith('.jar'));
      const pack = assets.find((a) => a.name.endsWith('.mrpack'));
      if (jar) {
        const btn = document.getElementById('download');
        btn.href = jar.browser_download_url;
        const mb = (jar.size / 1048576).toFixed(1);
        document.getElementById('dl-meta').textContent = `${rel.tag_name} · ${mb} MB · Minecraft 26.2 · Fabric`;
      }
      if (pack) document.getElementById('pack').href = pack.browser_download_url;
    })
    .catch(() => { /* the static links already point at the latest release */ });
})();
