// Offline cache so FUN CAM opens instantly from the Home Screen.
const CACHE = 'funcam-v2';
const FILES = ['./', 'index.html', 'app.js', 'gl.js', 'icons.js', 'roll.js', 'data.js', 'icon.svg', 'icon-180.png', 'manifest.webmanifest'];
self.addEventListener('install', e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)).then(() => self.skipWaiting())));
self.addEventListener('activate', e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  // network first so updates arrive, cache as fallback for offline use
  e.respondWith(fetch(e.request).then(r => { const copy = r.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); return r; })
    .catch(() => caches.match(e.request)));
});
