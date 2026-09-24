// Downloads the pyodide wheels loaded via pyodide.loadPackage (see
// src/renderer/shell/impl/python.cljs) into resources/public/pyodide,
// where the app serves its local pyodide index from. Wheels are fetched
// from the pyodide CDN and verified against the pyodide-lock.json sha256,
// since loadPackage rejects files that do not match the lockfile.
import { createHash } from "node:crypto";
import { existsSync } from "node:fs";
import { mkdir, readdir, readFile, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Packages the app loads with pyodide.loadPackage; dependencies are
// resolved transitively from the lockfile.
const required = ["jedi"];

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const pyodideDir = path.join(root, "resources", "public", "pyodide");
const lock = JSON.parse(
  await readFile(path.join(root, "node_modules", "pyodide", "pyodide-lock.json"), "utf8")
);
const pyodidePkg = JSON.parse(
  await readFile(path.join(root, "node_modules", "pyodide", "package.json"), "utf8")
);
const cdn = `https://cdn.jsdelivr.net/pyodide/v${pyodidePkg.version}/full/`;

const needed = new Map();
const visit = (name) => {
  const pkg = lock.packages[name];
  if (!pkg) throw new Error(`Unknown pyodide package: ${name}`);
  if (needed.has(name)) return;
  needed.set(name, pkg);
  for (const dep of pkg.depends) visit(dep);
};
for (const name of required) visit(name);

const sha256 = (buf) => createHash("sha256").update(buf).digest("hex");
const wheelNames = new Set([...needed.values()].map((p) => p.file_name));

await mkdir(pyodideDir, { recursive: true });
for (const [name, pkg] of needed) {
  const dest = path.join(pyodideDir, pkg.file_name);
  if (existsSync(dest) && sha256(await readFile(dest)) === pkg.sha256) {
    console.log(`up to date: ${pkg.file_name}`);
    continue;
  }
  const res = await fetch(cdn + pkg.file_name);
  if (!res.ok) {
    throw new Error(`Failed to download ${cdn + pkg.file_name}: HTTP ${res.status}`);
  }
  const buf = Buffer.from(await res.arrayBuffer());
  if (sha256(buf) !== pkg.sha256) {
    throw new Error(
      `sha256 mismatch for ${name}: expected ${pkg.sha256}, got ${sha256(buf)}`
    );
  }
  await writeFile(dest, buf);
  console.log(`downloaded: ${pkg.file_name}`);
}

// Drop wheels left over from older pyodide versions.
for (const file of await readdir(pyodideDir)) {
  if (file.endsWith(".whl") && !wheelNames.has(file)) {
    await rm(path.join(pyodideDir, file));
    console.log(`removed stale: ${file}`);
  }
}
