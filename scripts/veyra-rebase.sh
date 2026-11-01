#!/usr/bin/env bash
# veyra-rebase.sh — one-command upstream sync (master-first)
# Flow: upstream/master (DrKLO) → upstream (ff) → master (rebase on upstream)
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

echo "== fetch upstream =="
git remote add upstream https://github.com/DrKLO/Telegram.git 2>/dev/null || true
git fetch upstream master --tags --prune

NEW_SHA=$(git rev-parse upstream/master)
OLD_SHA=$(git rev-parse upstream 2>/dev/null || git rev-parse origin/upstream 2>/dev/null || echo "")
echo "upstream  $OLD_SHA -> $NEW_SHA"

if [ -n "$OLD_SHA" ] && [ "$NEW_SHA" = "$OLD_SHA" ]; then
  echo "already up to date — nothing to do"
  exit 0
fi

echo "== fast-forward upstream =="
git checkout upstream
git reset --hard upstream/master

echo "== rebase master onto upstream =="
git checkout master
if git rebase upstream; then
  echo "✅ rebase clean — no conflicts"
else
  echo ""
  echo "⚠️  CONFLICT — fix these files:"
  git diff --name-only --diff-filter=U || true
  echo ""
  echo "Then:"
  echo "  git add <fixed files>"
  echo "  git rebase --continue"
  echo "  # rerere is on — next time this conflict auto-resolves"
  echo "  git push --force-with-lease origin master"
  exit 1
fi

echo ""
echo "Done. branches:"
git log --oneline --graph -8
echo ""
echo "Push: git push --force-with-lease origin upstream master"
