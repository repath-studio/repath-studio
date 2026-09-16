// Generates src/generated/js_docs.json from the TypeScript lib declarations.
//
// Extracts the JSDoc and signature of every top-level global from the ES
// libs (ES built-ins), plus a set of common DOM globals, together with one
// level of members (e.g. Math.sin, console.log, document.createElement).
// The data is embedded into the shell-dsl namespace by
// build.generator.shell-dsl, so no doc text is written by hand and content
// follows the pinned `typescript` devDependency.
import { createRequire } from "node:module";
import path from "node:path";
import fs from "node:fs";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const ts = require("typescript");

const libRoot = path.dirname(require.resolve("typescript"));
const rootFile = path.join(libRoot, "lib.esnext.full.d.ts");

// DOM globals worth documenting in the shell, on top of the ES built-ins.
const DOM_GLOBALS = new Set([
  "alert",
  "atob",
  "btoa",
  "cancelAnimationFrame",
  "cancelIdleCallback",
  "clearInterval",
  "clearTimeout",
  "close",
  "confirm",
  "console",
  "crypto",
  "document",
  "fetch",
  "focus",
  "getComputedStyle",
  "getSelection",
  "history",
  "localStorage",
  "location",
  "matchMedia",
  "navigator",
  "open",
  "performance",
  "print",
  "prompt",
  "queueMicrotask",
  "requestAnimationFrame",
  "requestIdleCallback",
  "setInterval",
  "setTimeout",
  "sessionStorage",
  "structuredClone",
  "URL",
  "URLSearchParams",
  "window",
]);

const program = ts.createProgram([rootFile], {
  skipLibCheck: true,
  types: [],
  libRoot,
});
const checker = program.getTypeChecker();

const meaning =
  ts.SymbolFlags.Function |
  ts.SymbolFlags.Variable |
  ts.SymbolFlags.Class |
  ts.SymbolFlags.Enum |
  ts.SymbolFlags.Interface;

const globals = new Map();
for (const sf of program.getSourceFiles()) {
  for (const sym of checker.getSymbolsInScope(sf, meaning)) {
    if (!globals.has(sym.escapedName)) globals.set(sym.escapedName, sym);
  }
}

const isEsLibFile = (sf) => /^lib\.es/.test(path.basename(sf.fileName));
const kept = [...globals.values()].filter(
  (sym) =>
    (sym.declarations || []).some((d) => isEsLibFile(d.getSourceFile())) ||
    DOM_GLOBALS.has(sym.escapedName),
);

// Markdown-ish noise in the lib JSDoc is not useful in the plain-text docs
// panel, so links are reduced to their URL and emphasis markers dropped.
function clean(text) {
  return text
    .replace(/\[([^\]]*)\]\(([^)]+)\)/g, "$2")
    .replace(/\*\*/g, "")
    .replace(/`/g, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function docOf(sym) {
  const parts = sym.getDocumentationComment(checker);
  if (!parts || parts.length === 0) return undefined;
  const text = clean(parts.map((p) => p.text).join(""));
  return text || undefined;
}

function sigOf(sym) {
  if (!sym.valueDeclaration) return undefined;
  try {
    const sig = checker.getSignatureFromDeclaration(sym.valueDeclaration);
    return sig ? checker.signatureToString(sig) : undefined;
  } catch (e) {
    return undefined;
  }
}

function membersOf(sym) {
  const res = new Map();
  const add = (list) =>
    list.forEach((m) => {
      if (m.escapedName !== "prototype" && !res.has(m.escapedName))
        res.set(m.escapedName, m);
    });
  if (sym.flags & ts.SymbolFlags.Module) add(sym.getMembers());
  if (sym.valueDeclaration) {
    const t = checker.getTypeOfSymbolAtLocation(sym, sym.valueDeclaration);
    add(t.getProperties());
    for (const s of t.getConstructSignatures())
      add(checker.getReturnTypeOfSignature(s).getProperties());
  }
  return [...res.values()];
}

const out = {};
for (const sym of kept) {
  const name = sym.escapedName;
  const entry = {};
  const sig = sigOf(sym);
  const doc = docOf(sym);
  if (sig) entry.signature = sig;
  if (doc) entry.doc = doc;
  out[name] = entry;
  for (const m of membersOf(sym)) {
    const me = {};
    const ms = sigOf(m);
    const md = docOf(m);
    if (ms) me.signature = ms;
    if (md) me.doc = md;
    out[name + "." + m.escapedName] = me;
  }
}

const final = {};
for (const k of Object.keys(out).sort()) {
  if (out[k].doc || out[k].signature) final[k] = out[k];
}

const outFile = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../..",
  "src",
  "generated",
  "js_docs.json",
);
const json = JSON.stringify(final, null, 2) + "\n";
if (!fs.existsSync(outFile) || fs.readFileSync(outFile, "utf8") !== json) {
  fs.mkdirSync(path.dirname(outFile), { recursive: true });
  fs.writeFileSync(outFile, json);
  console.log(
    `[js-docs-generator] Generated ${outFile} (${Object.keys(final).length} names)`,
  );
}
