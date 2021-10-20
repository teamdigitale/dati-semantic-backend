#!/bin/bash
echo "Copies Talisman pre-commit hook to .git folder"
file='.git/hooks/pre-commit'

if [ ! -f "$file" ]
then
    echo 'No Talisman hook available. Setting up the hook now..'
    echo 'Copying Talisman pre-commit hook to your git hooks'
    cp tools/talisman/.githooks/pre-commit .git/hooks
    chmod +x .git/hooks/pre-commit
    echo 'Copying Talisman bin to your git hooks'
    mkdir -p .git/hooks/bin
    cp tools/talisman/.githooks/talisman .git/hooks/bin/

else
    echo 'A pre-commit hook already exists. Ensure Talisman check is also part of your pre-commit hook'
fi