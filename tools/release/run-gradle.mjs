import { spawn } from 'node:child_process';
import { platform } from 'node:os';

const task = process.argv[2];

if (!task) {
    console.error('Usage: node tools/release/run-gradle.mjs <task>');
    process.exit(1);
}

const isWindows = platform() === 'win32';
const command = isWindows ? 'cmd.exe' : './gradlew';
const args = isWindows ? ['/d', '/s', '/c', '.\\gradlew.bat', task] : [task];
const child = spawn(command, args, { stdio: 'inherit' });

child.on('exit', (code, signal) => {
    if (signal) {
        console.error(`Gradle ${task} was interrupted by ${signal}.`);
        process.exit(1);
    }

    process.exit(code ?? 1);
});
