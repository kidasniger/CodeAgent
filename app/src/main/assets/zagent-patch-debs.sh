#!@PREFIX@/bin/bash
# Adapte les paquets officiels Termux (com.termux) a cette appli (com.zagent).
# Appele par apt (DPkg::Pre-Install-Pkgs) : la liste des fichiers .deb arrive sur stdin.
export PATH="@PREFIX@/bin"
export LC_ALL=C
OLD="com.termux"
NEW="com.zagent"
status=0

# err-020 : outils requis presents — sinon message clair et echec propre.
for tool in dpkg-deb sed grep find xargs chmod mktemp; do
  command -v "$tool" >/dev/null 2>&1 || {
    echo "zagent: outil manquant : $tool — le paquet ne sera pas adapte" >&2
    exit 1
  }
done

n_ok=0
n_echec=0

fail() {
  echo "zagent: $1" >&2
  shift
  [ -n "$1" ] && printf '%s\n' "$1" | head -n 8 >&2
  status=1
  n_echec=$((n_echec + 1))
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

  # Scripts de maintenance : dpkg-deb exige des droits executables (>= 0555).
  for s in preinst postinst prerm postrm config; do
    [ -f "$work/DEBIAN/$s" ] && chmod 0755 "$work/DEBIAN/$s"
  done

  # Reconstruction : gzip (rapide), puis repli sur la compression par defaut.
  if err="$(dpkg-deb -Zgzip -z3 -b "$work" "$deb.patched" 2>&1)" || \
     err="$(dpkg-deb -b "$work" "$deb.patched" 2>&1)"; then
    mv -f "$deb.patched" "$deb"
    n_ok=$((n_ok + 1))
  else
    fail "echec de la reconstruction de $(basename "$deb")" "$err"
    rm -f "$deb.patched"
    # err-020 : on garde le .deb original intact — apt l'installera non adapte
    # plutot que de perdre le paquet entier.
  fi
  rm -rf "$work"
done

# err-020 : resume lisible en fin de traitement.
echo "zagent: paquets adapses : $n_ok ok, $n_echec echec(s)" >&2
exit $status
