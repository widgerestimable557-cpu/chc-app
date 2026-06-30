#!/usr/bin/env python3
"""
Génère toutes les icônes Android (legacy + adaptive) à partir d'un logo source.
- Isole uniquement le cercle du logo (ignore le bloc de texte séparé en dessous)
- Recentre et redimensionne avec un padding sûr pour les adaptive icons
- Génère mipmap-mdpi à xxxhdpi (ic_launcher.png, ic_launcher_round.png, ic_launcher_foreground.png)

Usage:
    python3 generate_icons.py <chemin_image_source> [chemin_dossier_res_android]
"""

import sys
import os
from PIL import Image
import numpy as np

LAUNCHER_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def autocrop(img: Image.Image, bg_threshold: int = 245) -> Image.Image:
    rgba = img.convert("RGBA")
    arr = np.array(rgba)

    if arr.shape[2] == 4:
        alpha = arr[:, :, 3]
        rgb = arr[:, :, :3]
        is_white = np.all(rgb >= bg_threshold, axis=2)
        is_transparent = alpha < 10
        mask = ~(is_white | is_transparent)
    else:
        rgb = arr[:, :, :3]
        mask = ~np.all(rgb >= bg_threshold, axis=2)

    rows = np.any(mask, axis=1)
    cols = np.any(mask, axis=0)

    if not rows.any() or not cols.any():
        print("ATTENTION: aucun contenu détecté lors du crop, image inchangée.")
        return rgba

    rmin, rmax = np.where(rows)[0][[0, -1]]
    cmin, cmax = np.where(cols)[0][[0, -1]]

    return rgba.crop((cmin, rmin, cmax + 1, rmax + 1))


def crop_top_blob(img: Image.Image, bg_threshold: int = 245, gap_min_px: int = 6) -> Image.Image:
    """
    Isole uniquement le premier bloc de contenu en partant du haut (le cercle du logo),
    en ignorant tout bloc séparé par un espace vide en dessous (ex: texte "CHC HAITI").
    """
    rgba = img.convert("RGBA")
    arr = np.array(rgba)

    alpha = arr[:, :, 3]
    rgb = arr[:, :, :3]
    is_white = np.all(rgb >= bg_threshold, axis=2)
    is_transparent = alpha < 10
    mask = ~(is_white | is_transparent)

    row_has_content = np.any(mask, axis=1)

    if not row_has_content.any():
        print("ATTENTION: aucun contenu détecté, image inchangée.")
        return rgba

    first_content_row = np.argmax(row_has_content)

    consecutive_empty = 0
    end_row = arr.shape[0]
    for r in range(first_content_row, arr.shape[0]):
        if row_has_content[r]:
            consecutive_empty = 0
        else:
            consecutive_empty += 1
            if consecutive_empty >= gap_min_px:
                end_row = r - consecutive_empty + 1
                break

    top_blob = rgba.crop((0, first_content_row, arr.shape[1], end_row))
    return autocrop(top_blob, bg_threshold=bg_threshold)


def square_pad(img: Image.Image, padding_ratio: float = 0.0) -> Image.Image:
    w, h = img.size
    side = max(w, h)
    side = int(side * (1 + padding_ratio))

    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    offset = ((side - w) // 2, (side - h) // 2)
    canvas.paste(img, offset, img)
    return canvas


def generate_legacy_icons(square_img: Image.Image, res_dir: str):
    for folder, size in LAUNCHER_SIZES.items():
        target_dir = os.path.join(res_dir, folder)
        os.makedirs(target_dir, exist_ok=True)

        resized = square_img.resize((size, size), Image.LANCZOS)
        resized.save(os.path.join(target_dir, "ic_launcher.png"))
        resized.save(os.path.join(target_dir, "ic_launcher_round.png"))
        print(f"  -> {folder}/ic_launcher.png ({size}x{size})")


def generate_foreground_icons(square_img: Image.Image, res_dir: str):
    for folder, size in FOREGROUND_SIZES.items():
        target_dir = os.path.join(res_dir, folder)
        os.makedirs(target_dir, exist_ok=True)

        inner_size = int(size * 0.66)
        resized_logo = square_img.resize((inner_size, inner_size), Image.LANCZOS)

        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        offset = ((size - inner_size) // 2, (size - inner_size) // 2)
        canvas.paste(resized_logo, offset, resized_logo)

        canvas.save(os.path.join(target_dir, "ic_launcher_foreground.png"))
        print(f"  -> {folder}/ic_launcher_foreground.png ({size}x{size}, logo {inner_size}x{inner_size})")


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 generate_icons.py <image_source> [res_dir]")
        sys.exit(1)

    src_path = sys.argv[1]
    res_dir = sys.argv[2] if len(sys.argv) > 2 else "android/app/src/main/res"

    if not os.path.isfile(src_path):
        print(f"ERREUR: fichier introuvable -> {src_path}")
        sys.exit(1)

    print(f"Chargement de {src_path}...")
    img = Image.open(src_path)

    print("Isolation du cercle (logo seul, sans le bloc de texte séparé)...")
    cropped = crop_top_blob(img)
    print(f"  Taille après crop: {cropped.size}")

    print("Mise en canvas carré centré...")
    squared_legacy = square_pad(cropped, padding_ratio=0.08)
    squared_foreground = square_pad(cropped, padding_ratio=0.0)

    print("Génération des icônes legacy (ic_launcher / ic_launcher_round)...")
    generate_legacy_icons(squared_legacy, res_dir)

    print("Génération des icônes foreground (adaptive icons)...")
    generate_foreground_icons(squared_foreground, res_dir)

    preview_path = "icon_preview_cropped.png"
    squared_legacy.resize((512, 512), Image.LANCZOS).save(preview_path)
    print(f"\nTerminé. Preview disponible: {preview_path}")
    print("Vérifie le rendu avant de commit/push.")


if __name__ == "__main__":
    main()
