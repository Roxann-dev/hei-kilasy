# Journal IA — Kilasy

**Outil utilisé :** Claude Code (Claude Opus 5.5), dans le terminal
**Règle que je me suis fixée :** l'IA ne modifie aucun fichier sans
que j'aie validé son plan.

---

## C1 — Pénalité de retard

> **Demande du client :** −10 % de la note par jour de retard entamé,
> avec un plafond de −50 %.

### En résumé

| | |
|---|---|
| **Fichiers modifiés** | `SubmissionController.java`, `GradeReport.java`, `CsvExport.java` |
| **Bug existant corrigé** | le CSV tronquait la note alors que le reste de l'appli l'arrondissait |
| **Ajouts** | champ `latePenaltyPercent` dans le JSON, ligne sur la pénalité dans l'email |
| **Limite constatée** | la même formule est recopiée à 4 endroits |

---

### Échange 1 · Analyse du code

**Demande :** analyser le code avant de proposer quoi que ce soit.

**Proposition :** l'IA a trouvé que la note sur 20 est calculée à
**4 endroits**, et que le CSV n'arrondit pas comme les autres :

| Endroit | Arrondi |
|---|---|
| `SubmissionController.get` | au plus proche |
| `SubmissionController.grade` (et email) | au plus proche |
| `GradeReport` (relevé) | au plus proche |
| `CsvExport` | **troncature** |

Par exemple, Emma a 27,5/40 : elle a 13,8 dans son relevé et 13,7 dans
le CSV.

**Décision :** gardé. Je corrige cette incohérence dans le cadre de C1.

**Pourquoi :** le critère « même note partout » n'était déjà pas respecté.
Ajouter la pénalité sans corriger ça aurait laissé le bug.

---

### Échange 2 · Comment compter les jours de retard

**Proposition :** compter les jours calendaires entre l'échéance et la
date de soumission, comme le code le fait déjà pour marquer une
soumission « en retard ».

**Décision :** gardé.

**Pourquoi :** c'est cohérent avec la règle existante, et ça redonne
exactement les exemples du client sur les données de démo (Bob, Clara,
David).

---

### Échange 3 · Où mettre le calcul, et quel arrondi

| Question | Options proposées | Mon choix |
|---|---|---|
| Où mettre la formule ? | méthode partagée / copie aux 4 endroits | **copie aux 4 endroits** |
| Quel arrondi ? | au plus proche / troncature | **au plus proche, 1 décimale** |

**Pourquoi :**
- La consigne interdit de réorganiser le code. J'ai préféré la respecter
  strictement, même si ça veut dire dupliquer la formule.
- 3 endroits sur 4 arrondissaient déjà au plus proche, donc les notes
  déjà envoyées par email ne changent pas.
- `BigDecimal` partout plutôt que `double` : avec `double`, une note
  comme x,x5 peut s'arrondir différemment selon le fichier.

---

### Échange 4 · Validation du plan

**Demande :** « Avant de coder, montre-moi ce que tu vas faire. Jamais
de modification sans mon accord. »

**Proposition :** un plan fichier par fichier, avec un tableau des notes
attendues **avant** de coder.

**Décision :** validé, avec deux ajouts (échange 5).

---

### Échange 5 · Afficher la pénalité ?

J'ai demandé à l'IA ce qu'elle recommandait.

| Proposition | Décision | Pourquoi |
|---|---|---|
| Champ `latePenaltyPercent` dans le JSON | gardé | l'enseignant comprend pourquoi la note a baissé |
| Ligne sur la pénalité dans l'email, seulement en cas de retard | gardé | sinon l'étudiant croit à une erreur de note |
| Colonne de pénalité dans le CSV | **refusé** | la scolarité l'ouvre dans Excel, et le client ne l'a pas demandée |

---

### Échange 6 · Codage et vérification

**Ce que l'IA a fait :** modifié les 3 fichiers, compilé, remis la base
à zéro, puis comparé les résultats au tableau prévu.

**Résultats :**

| Soumission | Retard | Correction | Relevé | CSV |
|---|---|---|---|---|
| Alice, Tri à bulles | 0 j | 16,0 | 16,0 | 16,0 |
| Bob, Tri à bulles | 1 j | 14,4 | 14,4 | 14,4 |
| Clara, Tri à bulles | 3 j | 11,2 | 11,2 | 11,2 |
| David, Tri à bulles | 8 j | 8,0 | 8,0 | 8,0 |
| Emma, Requêtes SQL | 0 j | 13,8 | 13,8 | 13,8 ✔ (13,7 avant) |

Bob, Pile et file, noté 10/10 avec 1 jour de retard : **18,0** partout,
et l'email affiche « Pénalité de retard : −10 % ».

**Ma vérification :** j'ai comparé ces résultats au tableau des notes
attendues, fixé avant le codage : tout correspond, y compris les
exemples du client (14,4, 11,2 et 8). Les trois critères d'acceptation
sont respectés : même note partout, pas de pénalité pour une soumission
rendue à l'heure, et pas de note négative possible (points bornés entre
0 et le barème, pénalité plafonnée à 50 %).

---

### Ce que je retiens

Une règle simple (« −10 % par jour ») m'a obligé à modifier **4 endroits**
dans **3 fichiers**, et l'un d'eux était déjà faux. Si le client change
la règle, il faudra penser à modifier les 4 endroits, sinon les notes ne
seront plus les mêmes partout.
