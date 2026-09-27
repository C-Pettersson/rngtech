import { readFileSync, writeFileSync } from 'node:fs';

const version = process.argv[2];

if (!version) {
    console.error('Usage: node tools/release/update-gradle-version.mjs <version>');
    process.exit(1);
}

const file = 'gradle.properties';
const contents = readFileSync(file, 'utf8');
const nextContents = contents.replace(/^mod_version=.*$/m, `mod_version=${version}`);

if (contents === nextContents) {
    console.error(`Could not find mod_version in ${file}.`);
    process.exit(1);
}

writeFileSync(file, nextContents);
