# Translations

`en-US.edn` is the base translation file. Every other file in this directory
must define exactly the same set of keys. New keys are added to `en-US.edn`
first; the other language files follow. LLM-generated translations are allowed
(see LLM_POLICY.md).

## Detecting missing keys

    clojure -M -m build.translation-validator --report

For each language file that is missing keys, the report lists each missing
key with its English source value and an insertion anchor (the previous key
already present in that file, or the namespace after which a whole new
namespace map should be inserted). Extra keys — present in a language file
but absent from `en-US.edn` — are listed separately; surface them to the
maintainer, do not remove them.

## Translating

- Translate the reported English value into the target language, using the
  surrounding entries of the same file as a reference for tone and
  terminology.
- Preserve `%1`-style placeholders and their order.
- Values that are vectors (e.g. `[:div "Customize shortcuts for %1"]`) keep
  their structure and tag names; translate only the string parts.
- Keep inline HTML/SVG tags (`<animate>`, `<circle>`, ...) intact.
- Insert keys at the reported anchor position. Never reorder, reword, or
  reformat existing translations.
- The files are hand-formatted EDN: match the 2-space indentation and wrap
  long strings so continuation lines align under the opening quote.

## Verifying

- `npm run validate-translations` must pass before finishing.
