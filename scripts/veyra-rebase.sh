#!/usr/bin/env bash
# veyra-rebase.sh — یک دستوری آپدیت veyra روی آخرین upstream
# Usage: ./scripts/veyra-rebase.sh
# این اسکریپت فقط master رو به upstream/master میبره و veyra رو روش rebase میکنه
# هیچوقت compat/boringssl/LAYER رو به عنوان فیچر کپی نمیکنه — upstream خودش داره

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

echo "== fetch upstream =="
git fetch upstream --tags --prune

NEW_SHA=$(git rev-parse upstream/master)
OLD_SHA=$(git rev-parse master)
echo "master  $OLD_SHA -> $NEW_SHA"

if [ "$NEW_SHA" = "$OLD_SHA" ]; then
  echo "already up to date — nothing to do"
  exit 0
fi

# 1) clean base forward
echo "== fast-forward master =="
git checkout master
git reset --hard upstream/master

# 2) rebase feature branch
echo "== rebase veyra onto master =="
git checkout veyra
if git rebase master; then
  echo "✅ rebase clean — no conflicts"
else
  echo ""
  echo "⚠️  CONFLICT — فقط همین فایل‌ها رو دستی درست کن:"
  git diff --name-only --diff-filter=U
  echo ""
  echo "بعدش:"
  echo "  git add <fixed files>"
  echo "  git rebase --continue"
  echo "  # rerere روشنه — دفعه بعد همین کانفلیکت اتومات حل میشه"
  exit 1
fi

echo ""
echo "Done. branches:"
git log --oneline --graph --all -8
echo ""
echo "اگر خواستی پوش کنی: git push -f origin veyra master"
