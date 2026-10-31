#!@PREFIX@/bin/bash
# Adapte les paquets officiels Termux (com.termux) a cette appli (com.zagent).
# Appele par apt (DPkg::Pre-Install-Pkgs) : la liste des fichiers .deb arrive sur stdin.
export PATH="@PREFIX@/bin"
export LC_ALL=C
status=0
while IFS= read -r deb; do
  [ -f "$deb" ] || continue
  work="$(mktemp -d "${TMPDIR:-@PREFIX@/tmp}/zpatch.XXXXXX")" || { status=1; continue; }
  if dpkg-deb -R "$deb" "$work" 2>/dev/null; then
    if [ -d "$work/data/data/com.termux" ]; then
      mv "$work/data/data/com.termux" "$work/data/data/com.zagent"
    fi
    find "$work" -type f -exec sed -i 's/com\.termux/com.zagent/g' {} +
    if dpkg-deb -b "$work" "$deb.patched" >/dev/null 2>&1; then
      mv -f "$deb.patched" "$deb"
    else
      echo "zagent: echec de la reconstruction de $deb" >&2
      status=1
    fi
  else
    echo "zagent: echec de l'extraction de $deb" >&2
    status=1
  fi
  rm -rf "$work"
done
exit $status
