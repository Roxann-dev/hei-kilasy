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

---

## C2 — Notification par SMS

> **Demande du client :** quand une note est publiée, l'étudiant reçoit
> aussi un SMS (simulé dans les logs) au numéro de son profil.

### En résumé

| | |
|---|---|
| **Fichiers modifiés** | `KilasyApplication.java`, `SubmissionController.java` |
| **Fichiers créés** | aucun |
| **Ajouts** | interface `SmsSender` + simulation dans les logs, champ `smsSent` dans le JSON |
| **Problème existant repéré (non corrigé)** | une panne de l'email renvoie un 500 alors que la note est déjà enregistrée |

---

### Échange 1 · Analyse du code

**Proposition :** l'IA a vérifié que C1 était bien poussé, puis a relevé :
- le numéro de téléphone est déjà lu par la requête de `grade()`, mais
  il ne servait à rien jusqu'ici ;
- la note est enregistrée en base **avant** l'envoi de l'email : si le
  SMS lève une exception sans qu'on l'attrape, l'API renvoie 500 alors
  que la note est publiée ;
- l'exemple du client écrit `14,4/20` (virgule), alors que l'email écrit
  `14.4/20`.

**Décision :** gardé. Le SMS suit le format du client, et l'email ne
change pas.

---

### Échange 2 · Un conflit entre la demande et la consigne

Le 3ᵉ critère (« changer de fournisseur sans modifier le code de la
notation ») demande de séparer l'envoi du SMS de la notation. Mais la
consigne du module interdit de créer des fichiers.

| Option proposée | Critère 3 | Consigne | Mon choix |
|---|---|---|---|
| `println` directement dans `grade()` | ✘ | ✔ | |
| Nouveaux fichiers `SmsSender.java` + `LogSmsSender.java` | ✔ | ✘ | |
| Interface + `@Bean` dans `KilasyApplication.java` | ✔ | ✔ | **✔** |

**Pourquoi :** c'est la seule option qui respecte les deux à la fois.
`SubmissionController` ne connaît que l'interface `SmsSender`. Le jour
où le client choisira un fournisseur, on changera seulement le `@Bean`.

---

### Échange 3 · Plan et comportement en cas d'échec

**Proposition :** l'envoi du SMS se fait dans un `try/catch` : en cas
d'échec, on l'écrit dans les logs et on renvoie quand même 200. Pour un
étudiant sans numéro, on n'envoie rien et on l'écrit dans les logs.

**Décision :** validé, avec le champ `smsSent` dans la réponse JSON.

**Pourquoi :** sans ce champ, l'enseignant ne peut pas savoir que le SMS
n'est pas parti.

**Refusé (hors sujet) :** corriger le 500 en cas de panne de l'email.
Le problème existait déjà et le client ne l'a pas demandé.

---

### Échange 4 · Test de panne

| Proposition | Mon choix | Pourquoi |
|---|---|---|
| Ajouter un `throw` temporaire, puis le retirer | **✔** | le code livré reste propre, et c'est ce que suggère l'énoncé |
| Une option de configuration `kilasy.sms.fail` | | du code qui ne sert qu'aux tests resterait en production |

---

### Échange 5 · Codage et vérification

**Ce que l'IA a fait :** codé, compilé, remis la base à zéro, puis
lancé les tests.

| Test | Résultat |
|---|---|
| Noter Bob (10/10) | email reçu + `[SMS] +261 34 00 000 02 : Votre note pour « Pile et file » est disponible : 18,0/20.` |
| **Panne simulée**, noter Felix (30/40) | réponse 200, `smsSent: false`, erreur écrite dans les logs, **note 15,0 bien publiée** (détail + CSV), email reçu |
| `throw` retiré | absent du `git diff`, le SMS repart normalement |
| Étudiant sans numéro | 200, `smsSent: false`, « pas de numéro » dans les logs |
| Non-régression C1 | Bob, soumission 2 : toujours 14,4 |

**Ma vérification :** j'ai comparé ces résultats au tableau attendu,
fixé avant le codage. Les trois critères sont respectés.

---

### Ce que je retiens

Le 3ᵉ critère m'a obligé à séparer **ce qu'on fait** (prévenir
l'étudiant) de **comment on le fait** (quel fournisseur). Avec la
consigne « pas de nouveau fichier », j'ai dû mettre l'interface dans
`KilasyApplication.java`, ce qui n'est pas sa place naturelle. Ça
montre la limite : sans pouvoir réorganiser le code, chaque nouvelle
dépendance externe finit dans un endroit qui n'est pas fait pour elle.

---

## C3 — Exercices minutés

> **Demande du client :** un exercice peut avoir une durée en minutes.
> Le décompte démarre quand l'étudiant ouvre l'exercice
> (`POST /exercises/{id}/start`), et il doit soumettre avant la fin.

### En résumé

| | |
|---|---|
| **Fichiers modifiés** | `schema.sql`, `Exercise.java`, `ExerciseController.java`, `SubmissionController.java`, `GradeReport.java`, `CsvExport.java`, `README.md` |
| **Fichiers créés** | aucun |
| **En base** | échéance facultative, colonne `duration_minutes`, nouvelle table `exercise_start` |
| **Limite repérée (non corrigée)** | `GET /exercises/{id}` montre l'énoncé sans démarrer le décompte |

---

### Échange 1 · Analyse du code

**Proposition :** l'IA a vérifié que C2 était bien poussé, puis a relevé
que le critère « une durée **à la place** de l'échéance » oblige à rendre
l'échéance facultative. Or le code suppose partout qu'elle existe :
`deadline` apparaît **37 fois dans 5 fichiers**, et chaque endroit
planterait avec une échéance vide, y compris **les 4 calculs de
pénalité de C1**.

**Décision :** gardé. J'ai rendu l'échéance facultative et protégé
chaque endroit qui l'utilise.

**Pourquoi :** sans ça, le premier critère n'est respecté qu'à moitié.
Et c'est exactement ce que j'avais noté à la fin de C1 : la formule
recopiée à 4 endroits m'a obligé à repasser sur les 4.

---

### Échange 2 · Les cas limites

| Question | Options proposées | Mon choix | Pourquoi |
|---|---|---|---|
| Deuxième appel à `/start` | renvoyer le démarrage existant / refuser (409) | **renvoyer l'existant (200)** | le décompte ne repart jamais à zéro, donc pas de triche, et recharger la page ne provoque pas d'erreur |
| Soumission sans `/start` | refuser (400) / refuser (422) / accepter | **refuser (400)** | un code différent du 422 (« temps dépassé ») pour distinguer les deux cas |
| Données de démo | ajouter un exercice minuté / tester via l'API | **tester via l'API** | les données de démo et le README (« 5 exercices ») restent vrais |

---

### Échange 3 · Plan

**Proposition :** un plan fichier par fichier, avec la nouvelle table
`exercise_start` (un seul démarrage par étudiant et par exercice), les
codes de réponse pour chaque cas, et 9 tests avec leurs résultats
attendus.

**Décision :** validé, avec la mise à jour du README (nouveau point
d'entrée et nouvelles règles).

**Pourquoi :** le README est la seule documentation du projet. Un point
d'entrée qui n'y figure pas serait introuvable pour la personne qui
reprendra le code.

**Refusé (hors sujet) :**
- corriger la faille du `GET /exercises/{id}`, qui montre l'énoncé sans
  démarrer le décompte : sans authentification, on ne peut pas vraiment
  l'empêcher ;
- ajouter le temps utilisé dans le CSV : le client ne l'a pas demandé,
  et une colonne de plus pourrait casser l'import de la scolarité.

---

### Échange 4 · Codage et vérification

**Ce que l'IA a fait :** codé, compilé, recréé la base (le schéma a
changé), puis lancé un script de 30 vérifications. Pour tester le
dépassement de temps sans attendre 30 minutes, l'heure de démarrage a
été reculée directement en base.

| Test | Résultat |
|---|---|
| Exercices sans durée | identiques à avant (notes, relevé, CSV, soumission) |
| Quiz de 30 min sans échéance | créé, `deadline: null` |
| 2ᵉ appel à `/start` | 200, même heure de démarrage |
| Soumission après 31 min | **422** « Temps écoulé : la durée de 30 minutes s'est terminée le … » |
| Soumission sans `/start` | **400** « Démarrez d'abord l'exercice » |
| Minuté + échéance dépassée de 2 jours, 20/20 | **16,0** dans la correction, le relevé et le CSV |
| Relevé | `"timeUsed": "12 min 30 s"` |
| CSV | échéance vide pour le quiz, en-tête inchangé |

**Ma vérification :** j'ai comparé ces résultats au tableau attendu,
fixé avant le codage. Les cinq critères sont respectés.

---

### Ce que je retiens

C3 est le changement qui a le plus coûté : **7 fichiers** pour une seule
fonctionnalité. La raison n'est pas la fonctionnalité elle-même, mais le
fait que l'échéance obligatoire était une hypothèse cachée, répétée
partout dans le code. Et la pénalité de C1, recopiée à 4 endroits, a dû
être protégée 4 fois.

Sur les trois changements, j'ai vu le même schéma : une règle simple du
client se transforme en modifications éparpillées, parce que le code
n'a pas d'endroit unique pour « la note », « la notification » ou
« l'échéance ». C'est ce qu'une meilleure organisation du code devrait
résoudre.
