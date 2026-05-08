---
name: UI design philosophy — minimal & professional
description: User strongly prefers minimal, professional UI; remove decorative elements rather than stacking them
type: feedback
---

L'utilisateur veut un design UI **vraiment professionnel et épuré**, inspiré de Lunar Client. Quand il critique une UI, sa préférence va systématiquement à la **suppression** d'éléments plutôt qu'à leur amélioration.

**Why:** Sur GuiMainMenu, il a explicitement demandé de retirer (et non améliorer) : la ligne séparatrice grise avec rectangle rouge, le fond transparent (panel verre) derrière les boutons et la petite ligne rouge au-dessus, le glow rectangulaire derrière le logo. Quand le wordmark texte sous le logo n'était pas terrible, il fallait le simplifier à mort, pas le redécorer.

**How to apply:**
- Pour les UIs custom (menus, GUIs), partir d'un canevas vide et ajouter le strict minimum.
- Pas de panels translucides "glassmorphism" empilés, pas de lignes décoratives, pas de glows colorés en arrière-plan.
- Privilégier : logo image net, typo soignée (scale + letterspacing si fontRenderer vanilla), boutons plats, fond panorama assombri.
- Si un élément ne sert pas une fonction claire (lisibilité, hiérarchie, action), le supprimer.
- Réf esthétique : Lunar Client main menu — fond très sombre, logo central, boutons sobres, presque rien d'autre.

**MAIS attention — ne pas confondre minimal avec "supprimer aussi les éléments fonctionnels existants" :**
quand on ajoute un élément à une UI existante, il ne faut pas en profiter pour retirer des
éléments fonctionnels que l'utilisateur a déjà validés (ex : flèches `>` indiquant qu'une
ligne est cliquable dans GuiUISettings). Ajouter un nouvel élément (badge "i") =
trouver une place qui ne chevauche pas, **sans toucher** aux éléments existants. Si l'espace
manque, dire le problème ou demander, plutôt que supprimer un élément utile.
