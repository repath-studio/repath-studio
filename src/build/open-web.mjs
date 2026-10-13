// Watches the :renderer build via shadow-cljs and opens the web app in the
// default browser once the build reports completion.
import { spawn } from "node:child_process";
import open from "open";

const url = "http://localhost:8080/";
const ready = /\[:renderer\] Build completed\./;
const cmd = "npx shadow-cljs watch renderer";

const child = spawn(cmd, { stdio: ["inherit", "pipe", "pipe"], shell: true });

let opened = false;
const openOnce = () => {
  if (opened) return;
  opened = true;
  open(url).catch(() =>
    console.log(`Could not open a browser. Visit ${url} manually.`)
  );
};

// Mirror a child stream to the matching process stream, watching for the
// completion line (on either stdout or stderr) across chunk boundaries.
const wire = (stream, sink) => {
  let buffer = "";
  stream.on("data", (chunk) => {
    const text = chunk.toString();
    sink.write(text);
    buffer += text;
    if (!opened && ready.test(buffer)) openOnce();
    const nl = buffer.lastIndexOf("\n");
    if (nl >= 0) buffer = buffer.slice(nl + 1);
  });
};

wire(child.stdout, process.stdout);
wire(child.stderr, process.stderr);

child.on("error", (err) => {
  console.error(err);
  process.exit(1);
});

child.on("exit", (code) => process.exit(code ?? 0));

