'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const { ipcMain } = require('electron');

const fixtureDir = fs.mkdtempSync(path.join(os.tmpdir(), 'document-test-'));
const fixturePath = path.join(fixtureDir, 'fixture.rps');

const fixtureDocument =
    '{:version "100000.0.0"\n' +
    ' :elements {#uuid "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"\n' +
    '            {:tag :circle\n' +
    '             :attrs {:r "100"\n' +
    '                     :cx "100"\n' +
    '                     :cy "100"}}}}';

const writeFixture = () => {
    fs.mkdirSync(fixtureDir, { recursive: true });
    fs.writeFileSync(fixturePath, fixtureDocument, 'utf-8');
};

writeFixture();

ipcMain.handle('test-fixture-path', () => {
    if (!fs.existsSync(fixturePath)) {
        writeFixture();
    }
    return fixturePath;
});

ipcMain.handle('test-fixture-dir', () => {
    if (!fs.existsSync(fixtureDir)) {
        fs.mkdirSync(fixtureDir, { recursive: true });
    }
    return fixtureDir;
});

ipcMain.handle('test-fixture-cleanup', () => {
    fs.rmSync(fixtureDir, { recursive: true, force: true });
    return true;
});

require('../../resources/main.js');
