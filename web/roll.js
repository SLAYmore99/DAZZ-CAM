// Tiny IndexedDB store so shots survive closing the app until they are saved to Photos.
const DB = 'funcam', STORE = 'roll';
let dbp;
function db() {
  return dbp ||= new Promise((res, rej) => {
    const r = indexedDB.open(DB, 1);
    r.onupgradeneeded = () => r.result.createObjectStore(STORE, { keyPath: 'id' });
    r.onsuccess = () => res(r.result);
    r.onerror = () => rej(r.error);
  });
}
async function tx(mode, fn) {
  const d = await db();
  return new Promise((res, rej) => {
    const t = d.transaction(STORE, mode);
    const out = fn(t.objectStore(STORE));
    t.oncomplete = () => res(out && out.result);
    t.onerror = () => rej(t.error);
  });
}
export const add = item => tx('readwrite', s => s.put(item));
export const remove = id => tx('readwrite', s => s.delete(id));
export const all = async () => ((await tx('readonly', s => s.getAll())) || []).sort((a, b) => b.id - a.id);
