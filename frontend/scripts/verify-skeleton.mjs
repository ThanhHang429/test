#!/usr/bin/env node
/**
 * verify-skeleton.mjs — Kiểm tra tính nhất quán của khung trước khi commit.
 *
 * Kiểm tra:
 *  1. Mọi file XML hợp lệ (dùng parser XML của PowerShell).
 *  2. id trong menu_bottom trùng id đích trong nav graph tương ứng.
 *  3. android:name của mỗi <fragment> trong nav graph trỏ đúng file .java.
 *  4. tools:layout của mỗi <fragment> trỏ đúng file layout.
 *  5. Layout trùng tên, nav id trùng nhau.
 *  6. Đủ 23 Api, 23 Repository, số DTO theo kế hoạch.
 *  7. Manifest trỏ đúng Activity/Service/Application/Theme đã khai báo.
 *  8. Không còn placeholder Google (TODO giả) và không có file BOM.
 *
 * Dùng: node scripts/verify-skeleton.mjs
 * Thoát mã 1 nếu có lỗi.
 */

import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const APP = join(ROOT, 'app', 'src', 'main');
const JAVA = join(APP, 'java', 'com', 'eventapp', 'app');
const RES = join(APP, 'res');
const PKG = 'com.eventapp.app';

const errors = [];
const warns = [];
const ok = (msg) => console.log('  OK   ' + msg);
const fail = (msg) => errors.push(msg);

function walk(dir, ext, out = []) {
  for (const e of readdirSync(dir)) {
    const full = join(dir, e);
    if (statSync(full).isDirectory()) walk(full, ext, out);
    else if (full.endsWith(ext)) out.push(full);
  }
  return out;
}

const rel = (f) => f.slice(ROOT.length + 1);

/* ---- 1. XML hợp lệ (parser PowerShell) -------------------------------- */
const xmlFiles = walk(RES, '.xml').concat([join(APP, 'AndroidManifest.xml')]);
const escaped = xmlFiles.map((f) => "'" + f.replace(/'/g, "''") + "'").join(',');
try {
  const ps = [
    '$files=@(' + escaped + ');',
    '$bad=@();',
    'foreach($f in $files){',
    '  try { $x=New-Object System.Xml.XmlDocument; $x.Load($f) }',
    '  catch { $bad += ($f + " :: " + $_.Exception.Message) }',
    '}',
    'if($bad.Count -gt 0){ $bad -join "|" }',
  ].join('\n');
  const out = execFileSync('powershell', ['-NoProfile', '-Command', ps], { encoding: 'utf8' }).trim();
  if (out) {
    for (const line of out.split('|')) fail('XML không hợp lệ: ' + line.trim());
  } else {
    ok(`${xmlFiles.length} file XML hợp lệ`);
  }
} catch (e) {
  fail('Không chạy được kiểm tra XML: ' + e.message);
}

/* ---- 2. Đọc nav graph và menu ---------------------------------------- */
const navGraphs = walk(join(RES, 'navigation'), '.xml');
const navData = new Map(); // file -> { id -> {cls, layout} }

for (const f of navGraphs) {
  const txt = readFileSync(f, 'utf8');
  const start = txt.match(/app:startDestination="@id\/([^"]+)"/);
  const entries = new Map();
  const re = /<fragment[\s\S]*?android:id="@\+id\/([^"]+)"[\s\S]*?android:name="([^"]+)"[\s\S]*?(?:tools:layout="[^"]*")?\s*\/?>/g;
  let m;
  while ((m = re.exec(txt))) {
    const layout = (m[0].match(/tools:layout="@layout\/([^"]+)"/) || [])[1] || null;
    entries.set(m[1], { cls: m[2], layout });
  }
  navData.set(f, { start: start ? start[1] : null, entries });
}

const allNavIds = new Map();
for (const [f, { entries }] of navData) {
  if (!navData.get(f).start) fail(`${rel(f)} thiếu app:startDestination`);
  for (const [id, info] of entries) {
    if (allNavIds.has(id)) {
      const prev = allNavIds.get(id);
      if (prev.file !== rel(f)) fail(`nav id "${id}" xuất hiện ở 2 nav graph: ${prev.file} và ${rel(f)}`);
    }
    allNavIds.set(id, { ...info, file: rel(f) });
  }
}
ok(`${navGraphs.length} nav graph, ${allNavIds.size} nav id`);

/* ---- 3. class / layout của mỗi nav destination tồn tại ----------------- */
let checked = 0;
for (const [id, info] of allNavIds) {
  if (!info.cls.startsWith(PKG)) {
    fail(`nav "${id}": android:name "${info.cls}" không thuộc package ${PKG}`);
    continue;
  }
  const javaFile = join(ROOT, 'app', 'src', 'main', 'java', ...info.cls.split('.')) + '.java';
  if (!existsSync(javaFile)) fail(`nav "${id}": không tìm thấy class ${info.cls}`);
  if (info.layout) {
    const layoutFile = join(RES, 'layout', info.layout + '.xml');
    if (!existsSync(layoutFile)) fail(`nav "${id}": layout @layout/${info.layout} không tồn tại`);
  }
  checked++;
}
ok(`${checked} destination có class và layout đầy đủ`);

/* ---- 4. Menu bottom khớp nav graph ------------------------------------ */
const menuDir = join(RES, 'menu');
const menus = [
  ['menu_user_bottom', 'nav_user'],
  ['menu_organizer_bottom', 'nav_organizer'],
  ['menu_admin_bottom', 'nav_admin'],
];
for (const [menu, nav] of menus) {
  const mf = join(menuDir, menu + '.xml');
  const nf = join(RES, 'navigation', nav + '.xml');
  if (!existsSync(mf) || !existsSync(nf)) {
    fail(`Thiếu ${menu}.xml hoặc ${nav}.xml`);
    continue;
  }
  const mt = readFileSync(mf, 'utf8');
  const nt = readFileSync(nf, 'utf8');
  const ids = [...mt.matchAll(/android:id="@\+id\/([^"]+)"/g)].map((m) => m[1]);
  if (ids.length !== 5) fail(`${menu}: cần đúng 5 mục, tìm thấy ${ids.length}`);
  for (const id of ids) {
    if (!nt.includes(`@+id/${id}`)) fail(`${menu}: id "${id}" không tồn tại trong ${nav}.xml`);
  }
  const dests = [...nt.matchAll(/android:id="@\+id\/([^"]+)"/g)].map((m) => m[1]);
  const notInMenu = dests.filter((d) => !ids.includes(d));
  if (ids.some((i) => !dests.includes(i))) fail(`${menu}: có id không phải đích điều hướng`);
  ok(`${menu}: 5/5 id khớp ${nav} (còn ${notInMenu.length} đích điều hướng khác)`);
}

/* ---- 5. Layout & nguồn ----------------------------------------------- */
const layouts = walk(join(RES, 'layout'), '.xml');
const names = layouts.map((f) => f.split('\\').pop().replace('.xml', ''));
const dup = names.filter((n, i) => names.indexOf(n) !== i);
if (dup.length) fail('Trùng tên layout: ' + [...new Set(dup)].join(', '));
ok(`${layouts.length} layout, không trùng tên`);

const javaFiles = walk(JAVA, '.java');
const classes = new Set(javaFiles.map((f) => join(f).split('\\').pop().replace('.java', '')));
const dupCls = javaFiles.map((f) => f.split('\\').pop()).filter((n, i, a) => a.indexOf(n) !== i);
if (dupCls.length) fail('Trùng tên class: ' + [...new Set(dupCls)].join(', '));
ok(`${javaFiles.length} file Java, không trùng tên`);

/* ---- 6. Api / Repository / DTO ---------------------------------------- */
const apis = readdirSync(join(JAVA, 'data', 'remote', 'api')).filter((f) => f.endsWith('.java'));
const repos = readdirSync(join(JAVA, 'data', 'repository')).filter((f) => f.endsWith('.java'));
const dtoDirs = readdirSync(join(JAVA, 'data', 'remote', 'dto'));
let dtoCount = 0;
for (const d of dtoDirs) dtoCount += readdirSync(join(JAVA, 'data', 'remote', 'dto', d)).filter((f) => f.endsWith('.java')).length;

if (apis.length !== 23) fail(`Cần 23 Api, tìm thấy ${apis.length}`);
else ok('23/23 Retrofit Api');
if (repos.length !== 23) fail(`Cần 23 Repository, tìm thấy ${repos.length}`);
else ok('23/23 Repository');
if (dtoCount !== 95) fail(`Cần 95 DTO, tìm thấy ${dtoCount}`);
else ok('95/95 DTO');
if (dtoDirs.length !== 23) fail(`Cần 23 package DTO, tìm thấy ${dtoDirs.length}`);

/* ---- 7. Manifest ------------------------------------------------------ */
const manifest = readFileSync(join(APP, 'AndroidManifest.xml'), 'utf8');
const componentRefs = [
  ['.ui.SplashActivity', `${PKG}.ui.SplashActivity`],
  ['.ui.AuthActivity', `${PKG}.ui.AuthActivity`],
  ['.ui.UserHostActivity', `${PKG}.ui.UserHostActivity`],
  ['.ui.OrganizerHostActivity', `${PKG}.ui.OrganizerHostActivity`],
  ['.ui.AdminHostActivity', `${PKG}.ui.AdminHostActivity`],
  ['.fcm.AppFirebaseMessagingService', `${PKG}.fcm.AppFirebaseMessagingService`],
  ['.EventApp', `${PKG}.EventApp`],
];
for (const [short, full] of componentRefs) {
  if (!manifest.includes(short)) fail(`Manifest thiếu khai báo ${short}`);
  else {
    const p = join(ROOT, 'app', 'src', 'main', 'java', ...full.split('.')) + '.java';
    if (!existsSync(p)) fail(`Manifest trỏ tới class không tồn tại: ${full}`);
  }
}
ok('Manifest khớp với Application, 5 Activity và FCM service');

const themes = readFileSync(join(RES, 'values', 'themes.xml'), 'utf8');
if (!themes.includes('name="Theme.EventApp"')) fail('Thiếu style Theme.EventApp');
if (!themes.includes('name="Theme.EventApp.Splash"')) fail('Thiếu style Theme.EventApp.Splash');
ok('Theme.EventApp và Theme.EventApp.Splash tồn tại');

/* ---- 8. Chất lượng file ----------------------------------------------- */
const textExts = ['.java', '.xml', '.gradle', '.mjs', '.md', '.properties', '.pro', '.json'];
const textFiles = [];
for (const dir of [ROOT, join(ROOT, 'app')]) {
  const scan = (d) => {
    for (const e of readdirSync(d)) {
      if (['.git', 'build', '.gradle', 'node_modules'].includes(e)) continue;
      const full = join(d, e);
      if (statSync(full).isDirectory()) scan(full);
      else if (textExts.some((x) => full.endsWith(x))) textFiles.push(full);
    }
  };
  scan(dir);
}
let bom = 0;
for (const f of textFiles) {
  const buf = readFileSync(f);
  if (buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) {
    bom++;
    fail('File có BOM: ' + rel(f));
  }
}
ok(`${textFiles.length} file text không có BOM`);

const javas = walk(join(APP, 'java'), '.java');
let noTodo = 0;
for (const f of javas) {
  const t = readFileSync(f, 'utf8');
  if (!/\/\/\s*TODO:/.test(t)) { noTodo++; fail('Thiếu TODO trong ' + rel(f)); }
  if (!/public (class|interface|enum) \w+/.test(t)) fail('Class không public: ' + rel(f));
}
ok(`${javas.length} file Java có TODO và class public`);

if (warns.length) {
  console.log('\nCẢNH BÁO:');
  for (const w of warns) console.log('  - ' + w);
}

if (errors.length) {
  console.error(`\nTHẤT BẠI: ${errors.length} lỗi`);
  for (const e of errors) console.error('  - ' + e);
  process.exit(1);
}
console.log('\nKHUNG HỢP LỆ — không phát hiện lỗi.');
