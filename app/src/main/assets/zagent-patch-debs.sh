#!@PREFIX@/bin/bash
# Adapte les paquets officiels Termux (com.termux) a cette appli (com.zagent).
# Appele par apt (DPkg::Pre-Install-Pkgs) : la liste des fichiers .deb arrive sur stdin.
export PATH="@PREFIX@/bin"
export LC_ALL=C
OLD="com.termux"
NEW="com.zagent"
status=0

fail() {
  echo "zagent: $1" >&2
  shift
  [ -n "$1" ] && printf '%s\n' "$1" | head -n 8 >&2
  status=1
}

while IFS= read -r deb; do
  [ -f "$deb" ] || continue
  work="$(mktemp -d "${TMPDIR:-@PREFIX@/tmp}/zpatch.XXXXXX")" || { status=1; continue; }

  if ! err="$(dpkg-deb -R "$deb" "$work" 2>&1)"; then
    fail "echec de l'extraction de $(basename "$deb")" "$err"
    rm -rf "$work"
    continue
  fi

  # Droits : tout doit etre lisible/ecrivable par nous, DEBIAN et racine en 0755.
  chmod -R u+rwX "$work" 2>/dev/null
  chmod 0755 "$work" 2>/dev/null
  [ -d "$work/DEBIAN" ] && chmod 0755 "$work/DEBIAN" 2>/dev/null

  # Chemin d'installation.
  if [ -d "$work/data/data/$OLD" ]; then
    mv "$work/data/data/$OLD" "$work/data/data/$NEW"
  fi

  # Contenu : uniquement les fichiers qui contiennent com.termux (rapide, binaires compris).
  grep -rlaZ --exclude-dir=.git -e "$OLD" "$work" 2>/dev/null | xargs -0 -r sed -i "s/com\\.termux/com.zagent/g"

  # Liens symboliques dont la cible contient com.termux.
  find "$work" -type l 2>/dev/null | while IFS= read -r l; do
    t="$(readlink "$l")"
    case "$t" in
      *"$OLD"*) rm -f "$l"; ln -s "${t//$OLD/$NEW}" "$l" ;;
    esac
  done

  # Reconstruction : gzip (rapide), puis repli sur la compression par defaut.
  if err="$(dpkg-deb -Zgzip -z3 -b "$work" "$deb.patched" 2>&1)" || \
     err="$(dpkg-deb -b "$work" "$deb.patched" 2>&1)"; then
    mv -f "$deb.patched" "$deb"
  else
    fail "echec de la reconstruction de $(basename "$deb")" "$err"
    rm -f "$deb.patched"
  fi
  rm -rf "$work"
done
exit $status
