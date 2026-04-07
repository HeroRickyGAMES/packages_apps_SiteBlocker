#!/bin/bash
# setup.sh — Apply the libcore DNS-blocking patch and register SiteBlocker in
# the AOSP build. Run this once from the SiteBlocker repo root.
#
# Usage:
#   bash setup.sh [AOSP_ROOT]
#   Default AOSP_ROOT = ../../../  (relative to packages/apps/SiteBlocker/)

set -e

AOSP_ROOT="${1:-../../..}"
AOSP_ROOT="$(realpath "$AOSP_ROOT")"

echo "==> AOSP root: $AOSP_ROOT"

LIBCORE_FILE="$AOSP_ROOT/libcore/ojluni/src/main/java/java/net/Inet6AddressImpl.java"
PATCH_FILE="$(dirname "$0")/patches/0001-libcore-Inet6AddressImpl-site-blocker.patch"

if [ ! -f "$LIBCORE_FILE" ]; then
    echo "ERROR: Cannot find $LIBCORE_FILE"
    echo "       Make sure AOSP_ROOT points to your AOSP source tree."
    exit 1
fi

echo "==> Checking if patch is already applied..."
if grep -q "SITE_BLOCKER_FILE" "$LIBCORE_FILE"; then
    echo "    Patch already applied, skipping."
else
    echo "==> Applying libcore patch..."
    patch -p1 -d "$AOSP_ROOT" < "$PATCH_FILE"
    echo "    Done."
fi

echo ""
echo "==> Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Add to your device Makefile (e.g. device/<vendor>/<device>/device.mk):"
echo "       PRODUCT_PACKAGES += SiteBlocker"
echo ""
echo "  2. Or add to vendor/<your_rom>/config/common_packages.mk:"
echo "       PRODUCT_PACKAGES += SiteBlocker"
echo ""
echo "  3. Build:"
echo "       m SiteBlocker"
echo ""
echo "  The app will be installed to /system/priv-app/SiteBlocker/"
echo "  Access: open 'Bloqueador de Sites' from the app drawer, or via"
echo "  Settings > Rede e Internet > Bloqueador de Sites (if hooked)."
