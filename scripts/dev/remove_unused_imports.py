#!/usr/bin/env python3
"""
Remove unused imports from Kotlin files.
This script parses Kotlin files, identifies unused imports, and removes them.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Set, Tuple

def extract_imports_and_content(file_path: Path) -> Tuple[List[str], str, str]:
    """Extract package, imports, and remaining content from a Kotlin file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.split('\n')
    package_lines = []
    import_lines = []
    other_lines = []
    in_imports = False
    
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('package '):
            package_lines.append(line)
        elif stripped.startswith('import '):
            import_lines.append(line)
            in_imports = True
        elif in_imports and stripped == '':
            # Empty line after imports
            continue
        else:
            other_lines.append(line)
            if stripped:  # Non-empty line after imports
                in_imports = False
    
    package_section = '\n'.join(package_lines) if package_lines else ''
    content_without_imports = '\n'.join(other_lines)
    
    return import_lines, package_section, content_without_imports

def extract_imported_symbol(import_line: str) -> str:
    """Extract the symbol name from an import statement."""
    # Handle various import patterns:
    # import foo.bar.ClassName
    # import foo.bar.ClassName as Alias
    # import foo.bar.*
    match = re.search(r'import\s+([\w.]+?)(?:\s+as\s+(\w+))?$', import_line.strip())
    if match:
        full_path = match.group(1)
        alias = match.group(2)
        if alias:
            return alias
        # Return the last part of the import path
        return full_path.split('.')[-1] if '.' in full_path else full_path
    return ''

def is_symbol_used(symbol: str, content: str) -> bool:
    """Check if a symbol is used in the content."""
    if not symbol or symbol == '*':
        # Wildcard imports are harder to check, keep them for safety
        return True
    
    # Special case for Kotlin delegation imports (getValue, setValue)
    # These are used implicitly by the `by` keyword
    if symbol in ['getValue', 'setValue']:
        if re.search(r'\bby\s+', content):
            return True
    
    # Match symbol as a whole word (not part of another word)
    # Look for usage in various contexts
    patterns = [
        rf'\b{re.escape(symbol)}\b',  # Basic word boundary match
        rf'@{re.escape(symbol)}',      # Annotation usage
        rf': {re.escape(symbol)}\b',   # Type declaration
        rf'<{re.escape(symbol)}\b',    # Generic type parameter
    ]
    
    for pattern in patterns:
        if re.search(pattern, content):
            return True
    
    return False

def remove_unused_imports(file_path: Path, dry_run: bool = False) -> Tuple[int, List[str]]:
    """Remove unused imports from a Kotlin file."""
    try:
        import_lines, package_section, content = extract_imports_and_content(file_path)
        
        if not import_lines:
            return 0, []
        
        used_imports = []
        unused_imports = []
        
        for import_line in import_lines:
            symbol = extract_imported_symbol(import_line)
            if is_symbol_used(symbol, content):
                used_imports.append(import_line)
            else:
                unused_imports.append(import_line.strip())
        
        if not unused_imports:
            return 0, []
        
        if not dry_run:
            # Reconstruct file with used imports only
            new_content_parts = []
            
            if package_section:
                new_content_parts.append(package_section)
                new_content_parts.append('')
            
            if used_imports:
                new_content_parts.extend(used_imports)
                new_content_parts.append('')
            
            new_content_parts.append(content.lstrip('\n'))
            
            new_content = '\n'.join(new_content_parts)
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
        
        return len(unused_imports), unused_imports
    
    except Exception as e:
        print(f"Error processing {file_path}: {e}", file=sys.stderr)
        return 0, []

def find_kotlin_files(base_dirs: List[str]) -> List[Path]:
    """Find all Kotlin files in the specified directories."""
    kotlin_files = []
    for base_dir in base_dirs:
        path = Path(base_dir)
        if path.exists():
            kotlin_files.extend(path.rglob('*.kt'))
    return kotlin_files

def main():
    """Main function."""
    project_root = Path(__file__).parent.parent.resolve()
    os.chdir(project_root)
    
    # Directories to scan
    source_dirs = [
        'shared/src',
        'composeApp/src',
        'server/src',
        'bside-api/src'
    ]
    
    dry_run = '--dry-run' in sys.argv
    verbose = '--verbose' in sys.argv or dry_run
    
    print("🔍 Scanning for Kotlin files with unused imports...")
    if dry_run:
        print("   (DRY RUN - no files will be modified)")
    print()
    
    kotlin_files = find_kotlin_files(source_dirs)
    total_removed = 0
    files_modified = 0
    
    for file_path in kotlin_files:
        removed_count, unused = remove_unused_imports(file_path, dry_run)
        if removed_count > 0:
            files_modified += 1
            total_removed += removed_count
            try:
                rel_path = file_path.relative_to(project_root)
            except ValueError:
                # If relative_to fails, resolve both paths first
                rel_path = file_path.resolve().relative_to(project_root)
            if verbose:
                print(f"{'[DRY RUN] ' if dry_run else ''}Removed {removed_count} unused import(s) from {rel_path}")
                for imp in unused:
                    print(f"  - {imp}")
            else:
                print(f"{'[DRY RUN] ' if dry_run else ''}✓ {rel_path}: {removed_count} unused import(s)")
    
    print()
    print(f"{'[DRY RUN] ' if dry_run else ''}Summary:")
    print(f"  Files processed: {len(kotlin_files)}")
    print(f"  Files with unused imports: {files_modified}")
    print(f"  Total unused imports removed: {total_removed}")
    
    if dry_run and files_modified > 0:
        print()
        print("Run without --dry-run to actually remove unused imports")

if __name__ == '__main__':
    main()
