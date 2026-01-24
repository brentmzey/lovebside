#!/usr/bin/env node
/**
 * Code-HQ Board Manager
 * 
 * Helps manage stories, tasks, and kanban board state
 * Usage: node .code-hq/board.js [command]
 */

const fs = require('fs');
const path = require('path');

const KANBAN_PATH = path.join(__dirname, 'KANBAN.md');
const STORIES_PATH = path.join(__dirname, 'STORIES.md');
const TASK_PATH = path.join(__dirname, '..', '.gemini', 'antigravity', 'brain', 'e9de2573-c41f-4db6-a3c5-5597c4bd61fe', 'task.md');
const CONTEXT_PATH = path.join(__dirname, 'CONTEXT.md');

// ANSI color codes
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    red: '\x1b[31m',
    cyan: '\x1b[36m'
};

function readFile(filePath) {
    try {
        return fs.readFileSync(filePath, 'utf8');
    } catch (error) {
        return '';
    }
}

function countTasks(content, pattern) {
    const matches = content.match(pattern);
    return matches ? matches.length : 0;
}

function showStatus() {
    console.log(`\n${colors.cyan}════════════════════════════════════════${colors.reset}`);
    console.log(`${colors.cyan}   📊 B-Side Project Dashboard${colors.reset}`);
    console.log(`${colors.cyan}════════════════════════════════════════${colors.reset}\n`);

    // Read files
    const kanban = readFile(KANBAN_PATH);
    const tasks = readFile(TASK_PATH);
    const context = readFile(CONTEXT_PATH);

    // Parse KANBAN sections
    const p0Done = countTasks(kanban.match(/### ✅ DONE:([\s\S]*?)###/)?.[1] || '', /- \[x\]/g);
    const p0Next = countTasks(kanban.match(/### 🔄 NEXT:([\s\S]*?)###/)?.[1] || '', /- \[ \]/g);
    const p0After = countTasks(kanban.match(/### 🔄 AFTER:([\s\S]*?)##/)?.[1] || '', /- \[ \]/g);
    const p1Tasks = countTasks(kanban.match(/## 📋 P1([\s\S]*?)##/)?.[1] || '', /- \[ \]/g);

    // Parse task.md
    const totalTasks = countTasks(tasks, /- \[.\]/g);
    const completedTasks = countTasks(tasks, /- \[x\]/g);
    const inProgressTasks = countTasks(tasks, /- \[\/\]/g);
    const todoTasks = totalTasks - completedTasks - inProgressTasks;

    // Display metrics
    console.log(`${colors.green}✅ Completed Tasks:${colors.reset} ${completedTasks}/${totalTasks}`);
    console.log(`${colors.yellow}🔄 In Progress:${colors.reset}     ${inProgressTasks}`);
    console.log(`${colors.blue}📋 To Do:${colors.reset}           ${todoTasks}\n`);

    console.log(`${colors.cyan}P0 Critical Path:${colors.reset}`);
    console.log(`  ✅ Done: ${p0Done} items`);
    console.log(`  🔄 Next: ${p0Next} items`);
    console.log(`  🔄 After: ${p0After} items\n`);

    console.log(`${colors.cyan}P1 High Priority:${colors.reset} ${p1Tasks} items\n`);

    // Show current focus from CONTEXT
    const focusMatch = context.match(/## 🎯 Current Focus \(P0\)([\s\S]*?)---/);
    if (focusMatch) {
        console.log(`${colors.yellow}Current Focus:${colors.reset}`);
        const focus = focusMatch[1].trim().split('\n')[0];
        console.log(`  ${focus}\n`);
    }

    // Recent work
    const sessionMatch = context.match(/### (\d{4}-\d{2}-\d{2}) Session([\s\S]*?)(?=###|$)/);
    if (sessionMatch) {
        console.log(`${colors.green}Last Session:${colors.reset} ${sessionMatch[1]}`);
        const notes = sessionMatch[2].trim().split('\n').slice(0, 3).join('\n  ');
        console.log(`  ${notes}\n`);
    }

    console.log(`${colors.cyan}════════════════════════════════════════${colors.reset}\n`);
}

function listStories() {
    console.log(`\n${colors.cyan}📖 Active Stories${colors.reset}\n`);

    const stories = readFile(STORIES_PATH);
    const storyMatches = stories.matchAll(/## Story (\d+): (.*?)$/gm);

    for (const match of storyMatches) {
        const num = match[1];
        const title = match[2];

        // Find status in story
        const storySection = stories.substring(match.index);
        const nextStoryIndex = storySection.indexOf('\n## Story');
        const storyText = nextStoryIndex > 0
            ? storySection.substring(0, nextStoryIndex)
            : storySection;

        const completed = countTasks(storyText, /- \[x\]/g);
        const total = countTasks(storyText, /- \[.\]/g);
        const progress = total > 0 ? Math.round((completed / total) * 100) : 0;

        const status = progress === 100 ? `${colors.green}✅ DONE${colors.reset}`
            : progress > 0 ? `${colors.yellow}🔄 ${progress}%${colors.reset}`
                : `${colors.blue}📋 TODO${colors.reset}`;

        console.log(`  ${status} Story ${num}: ${title}`);
    }

    console.log('');
}

function help() {
    console.log(`
${colors.cyan}Code-HQ Board Manager${colors.reset}

${colors.yellow}Usage:${colors.reset}
  node .code-hq/board.js [command]

${colors.yellow}Commands:${colors.reset}
  status     Show project dashboard with metrics
  stories    List all stories with progress
  help       Show this help message

${colors.yellow}Workflow:${colors.reset}
  1. Check ${colors.green}status${colors.reset} to see current state
  2. Review ${colors.blue}KANBAN.md${colors.reset} P0 section for next task
  3. Work on task, update ${colors.blue}task.md${colors.reset} as you go
  4. Move completed P0 items to ✅ Done section
  5. Update ${colors.blue}CONTEXT.md${colors.reset} with session notes

${colors.yellow}For AI Agents:${colors.reset}
  Read ${colors.blue}CONTEXT.md${colors.reset} → ${colors.blue}KANBAN.md${colors.reset} → ${colors.blue}STORIES.md${colors.reset}
  Follow ${colors.blue}AGENT_HANDOFF.md${colors.reset} protocol
`);
}

// Main
const command = process.argv[2] || 'status';

switch (command) {
    case 'status':
        showStatus();
        break;
    case 'stories':
        listStories();
        break;
    case 'help':
    default:
        help();
}
