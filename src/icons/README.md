# Iconography

We are trying to create a non distracting and concise icon set.
In order to achieve this, our icons will be small, thin and single tone SVGs.

## Icon specs

- Size should be 17x17 pixels.
- Lines should be 1px wide.
- Elements should be converted to strokeless paths.
- Those paths should be united into a single path.
- All external edges should be rounded.
- The suggested distance between two shapes is two pixels in order to be
  distinguishable.
- Use of filters is prohibited.
- The exported SVG should be optimized (unneeded elements should be removed).

## Creating sharp icons

A common misconception is that vector icons look good regardless of their
rendered size. Although they behave a lot better than rasterized images when
resized, they can also end up slightly blurry. That is why we need to design the
icons in the exact size that they are going to be rendered. We also need to use
a pixel grid and make sure that we follow it as much us we can.

You can take a look at the icon-template.svg below

<img src="icon-template.svg" alt="drawing" width="200" height="200"/>

## Why 17 pixels?

A size near 16 pixels seems to be good for our case.
Since we decided to use one pixel wide lines, having an odd size would help us
render sharp 1px lines at the center of the icon.

## Sample source

This is how the source of an icon should look like

```XML
<svg width="17" height="17" version="1.1" viewBox="0 0 17 17" xmlns="http://www.w3.org/2000/svg">
    <path d="..."/>
</svg>
```
