import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const files = execFileSync('git', ['-c', `safe.directory=${root.replaceAll('\\', '/')}`, 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], {
    cwd: root,
    encoding: 'utf8'
}).split('\0').filter(Boolean);
const issues = [];
const privatePaths = /(?:^|\/)(?:\.obsidian|\.agents|\.codex|\.venv|venv|run|node_modules|public-release)(?:\/|$)|(?:^|\/)\.env(?:\..+)?$|\.(?:pem|key|p12|jks|log)$/i;
const patterns = [
    ['personal home path', /(?:[A-Z]:[\\/]Users[\\/][^\s\\/]+|\/(?:Users|home)\/[^\s/]+)/i],
    ['personal email', /[\w.+-]+@(?:outlook|hotmail|gmail|yahoo|icloud|protonmail|proton)\.[a-z]+/i],
    ['credential-shaped value', /(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----)/]
];

for (const file of files) {
    const fullPath = resolve(root, file);
    if (!existsSync(fullPath)) continue;
    if (privatePaths.test(file) && !file.endsWith('.env.example')) issues.push(`${file}: local/private file should not be tracked`);
    const data = readFileSync(fullPath);
    if (data.includes(0)) continue;
    const text = data.toString('utf8');
    for (const [kind, pattern] of patterns) {
        if (pattern.test(text)) issues.push(`${file}: possible ${kind} (value withheld)`);
    }
    if (!file.endsWith('.md')) continue;
    const prose = text.replace(/^\s*```[^\n]*\n[\s\S]*?^\s*```\s*$/gm, '');
    for (const match of prose.matchAll(/!?\[[^\]]*\]\(([^)]+)\)/g)) {
        const target = match[1].trim().replace(/^<|>$/g, '').split(/\s+["']/)[0].split('#')[0];
        if (!target || /^(?:[a-z]+:|\/\/)/i.test(target)) continue;
        const destination = resolve(dirname(fullPath), decodeURIComponent(target));
        if (!existsSync(destination)) issues.push(`${file}: missing link target ${target}`);
    }
}

if (issues.length) {
    console.error(issues.join('\n'));
    process.exitCode = 1;
} else {
    console.log(`Repository check passed (${files.length} indexed/untracked paths reviewed; deleted paths skipped).`);
}
