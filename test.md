# Tests a executer

## Auth / Inscription / Login

- [ ] Inscription Freelancer sans fichier.
- [ ] Inscription Freelancer avec photo de profil + CV PDF.
- [ ] Verifier dans Keycloak que le compte Freelancer est cree.
- [ ] Verifier dans `freelancer_db` que le profil Freelancer est cree avec `pfpUrl` et `cvUrl`.
- [ ] Verifier que l'ecran succes Freelancer redirige vers `/sign-in`.
- [ ] Login Freelancer avec email/mot de passe.
- [ ] Inscription Company sans logo.
- [ ] Inscription Company avec logo.
- [ ] Tenter une inscription Company avec un email deja present en base.
- [ ] Verifier le message: `Email deja utilise`.
- [ ] Verifier dans Keycloak que le compte Company est cree.
- [ ] Verifier dans `company_db` que le profil Company est cree avec statut `Pending`, `pfpUrl`, adresse, telephone et domaine.
- [ ] Verifier que l'ecran succes Company redirige vers `/sign-in`.
- [ ] Tenter login Company en statut `Pending`.
- [ ] Verifier le message: `Compte non verifie par l'administrateur, veuillez patienter.`
- [ ] Valider la Company depuis AdminPanel.
- [ ] Retenter login Company apres validation.

## Admin / Coherence Keycloak et bases metier

- [ ] Supprimer une Company depuis AdminPanel.
- [ ] Verifier suppression dans `company_db`.
- [ ] Verifier suppression dans Keycloak.
- [ ] Supprimer une Company deja absente de Keycloak mais presente en base.
- [ ] Verifier que la suppression en base fonctionne quand meme.
- [ ] Repeter le meme test pour Freelancer.

## Mot de passe oublie

- [x] Configurer les variables SMTP dans `.env` avec un mot de passe d'application, sans espaces.
- [x] Verifier que `auth-service` recoit les variables SMTP via `docker compose config auth-service`.
- [x] Redemarrer `auth-service` apres configuration SMTP.
- [ ] Verifier dans les logs `auth-service` qu'aucune erreur SMTP n'apparait au demarrage.
- [ ] Verifier que le mot de passe d'application SMTP contient 16 caracteres apres suppression des espaces.
- [ ] Si Gmail retourne `535 Username and Password not accepted`, regenerer un nouveau mot de passe d'application Google.
- [ ] Demander un OTP avec un email Freelancer existant.
- [ ] Demander un OTP avec un email Company existant.
- [ ] Demander un OTP avec un email inexistant.
- [ ] Verifier reception SMTP du code.
- [ ] Valider un OTP correct.
- [ ] Refuser un OTP incorrect.
- [ ] Refuser un OTP expire.
- [ ] Reinitialiser le mot de passe.
- [ ] Login avec l'ancien mot de passe: doit echouer.
- [ ] Login avec le nouveau mot de passe: doit fonctionner.

### Endpoints Postman via WSO2

Base URL: `http://localhost:8280/api/auth/v1`

1. Demander OTP:

```http
POST /password/forgot
Content-Type: application/json
```

```json
{
  "email": "email.existant@gmail.com"
}
```

2. Verifier OTP:

```http
POST /password/verify-otp
Content-Type: application/json
```

```json
{
  "email": "email.existant@gmail.com",
  "otp": "123456"
}
```

3. Reinitialiser:

```http
POST /password/reset
Content-Type: application/json
```

```json
{
  "email": "email.existant@gmail.com",
  "otp": "123456",
  "newPassword": "NouveauPassword123!",
  "confirmPassword": "NouveauPassword123!"
}
```

### Ressources WSO2 a ajouter dans AuthAPI

- [ ] `POST /password/forgot`
- [ ] `POST /password/verify-otp`
- [ ] `POST /password/reset`

Les endpoints internes `/api/companies/internal/email-exists` et `/api/freelances/internal/email-exists` ne doivent pas etre declares dans WSO2.

## Connexion avec OTP

- [ ] Login Freelancer avec email/mot de passe correct.
- [ ] Verifier qu'un OTP est envoye par email et que les tokens ne sont pas encore stockes.
- [ ] Saisir un OTP incorrect.
- [ ] Verifier le message: `Code OTP invalide.`
- [ ] Cliquer sur `Generer un nouveau code`.
- [ ] Verifier reception d'un nouveau code OTP.
- [ ] Saisir un OTP correct.
- [ ] Verifier que la connexion reussit et redirige vers `/freelancer`.
- [ ] Login Company en statut `Pending`.
- [ ] Verifier que le login est bloque avant l'envoi OTP avec le message de compte non verifie.
- [ ] Valider la Company depuis AdminPanel.
- [ ] Login Company validee, verifier OTP puis redirection vers `/company`.
- [ ] Login Admin, verifier OTP puis redirection vers `/admin`.

### Endpoints Postman via WSO2

Base URL: `http://localhost:8280/api/auth/v1`

1. Demarrer login:

```http
POST /login
Content-Type: application/json
```

```json
{
  "email": "email.existant@gmail.com",
  "password": "MotDePasse123!"
}
```

Reponse attendue:

```json
{
  "otpRequired": true,
  "challengeId": "uuid",
  "email": "email.existant@gmail.com",
  "message": "Code OTP envoye.",
  "expiresInSeconds": 600
}
```

2. Verifier OTP de connexion:

```http
POST /login/verify-otp
Content-Type: application/json
```

```json
{
  "challengeId": "uuid",
  "otp": "123456"
}
```

3. Generer un nouveau OTP:

```http
POST /login/resend-otp
Content-Type: application/json
```

```json
{
  "challengeId": "uuid"
}
```

### Ressources WSO2 a ajouter dans AuthAPI

- [ ] `POST /login/verify-otp`
- [ ] `POST /login/resend-otp`

## Freelancer / Recherche missions

- [ ] Charger l'espace Freelancer puis l'onglet Missions.
- [ ] Verifier que seules les missions publiees sont affichees.
- [ ] Filtrer par mode `Remote`.
- [ ] Filtrer par mode `Presentiel`.
- [ ] Filtrer par mode `Hybride`.
- [ ] Filtrer par competence, par exemple `Java` ou `React`.
- [ ] Filtrer par budget `Inferieur a 5 000 MAD`.
- [ ] Filtrer par budget `5 000 - 10 000 MAD`.
- [ ] Filtrer par budget `10 000 - 25 000 MAD`.
- [ ] Filtrer par budget `Superieur a 25 000 MAD`.
- [ ] Filtrer par intervalle budget personnalise.
- [ ] Rechercher par mot-cle dans le titre ou la description.
- [ ] Rechercher par technologie presente dans les competences.
- [ ] Rechercher par nom d'entreprise.
- [ ] Rechercher par domaine d'entreprise.
- [ ] Combiner mode + budget + competence + mot-cle.

### Endpoint Postman via WSO2

Base URL: `http://localhost:8280/api/missions/v1`

```http
GET /search?workMode=REMOTE&skill=Java&minBudget=5000&maxBudget=10000
```

## Freelancer / Profil et candidatures

- [ ] Modifier le profil Freelancer: photo, CV, nom, prenom, telephone, competences et experiences.
- [ ] Verifier que les changements sont sauvegardes dans `freelancer_db`.
- [ ] Verifier que nom, prenom et telephone sont synchronises dans Keycloak apres modification du profil.
- [ ] Supprimer la photo de profil puis verifier que l'interface n'affiche plus l'ancienne image.
- [ ] Supprimer le CV puis verifier que le lien CV disparait cote Freelancer et cote Company.
- [ ] Postuler a une mission sans lettre de motivation.
- [ ] Verifier qu'une conversation est creee automatiquement avec l'entreprise.
- [ ] Verifier que le message cree contient la mission concernee et le profil du freelancer.
- [ ] Postuler a une mission avec lettre de motivation.
- [ ] Verifier que la lettre apparait comme message dans la messagerie et qu'elle est liee a la mission.
- [ ] Cote Company, ouvrir l'onglet Candidatures.
- [ ] Verifier que la carte candidature affiche le profil complet du freelancer: photo, nom, email, telephone, competences, experiences et CV.
- [ ] Cote Company, tenter d'ouvrir directement le profil d'un freelancer qui n'a pas postule a une mission de l'entreprise.
- [ ] Verifier que l'acces est refuse.
- [ ] Modifier ou cloturer la mission cote Company.
- [ ] Verifier cote Freelancer que le statut ou la disponibilite de la mission est coherent apres rechargement.

## Freelancer / Paiement Stripe

- [ ] Remplacer `STRIPE_SECRET_KEY=sk_test_xxx` par une vraie cle secrete de test Stripe dans `.env`.
- [ ] Renseigner `VITE_STRIPE_PUBLISHABLE_KEY` avec la cle publique de test Stripe cote `B2B-Front-End/.env`.
- [ ] Verifier que `payment-service` recoit la vraie cle apres `docker compose up -d payment-service`.
- [ ] Verifier que `STRIPE_FEE_PERCENT` contient la commission plateforme attendue.
- [ ] Verifier que `STRIPE_DEFAULT_CURRENCY=mad`.
- [ ] Verifier que `STRIPE_ACCOUNT_LINK_RETURN_URL` et `STRIPE_ACCOUNT_LINK_REFRESH_URL` pointent vers le frontend actif.
- [ ] Cliquer sur `Connecter mon compte Stripe`.
- [ ] Si Stripe n'est pas configure, verifier que le message affiche indique de renseigner `STRIPE_SECRET_KEY`.
- [ ] Avec une vraie cle Stripe, verifier que l'utilisateur est redirige vers l'onboarding Stripe Express.
- [ ] Terminer ou quitter l'onboarding puis verifier le retour vers l'espace Freelancer.
- [ ] Recliquer sur `Connecter mon compte Stripe` apres creation du compte et verifier qu'un nouveau lien onboarding est genere.

## Company / Profil

- [ ] Modifier le profil Company: nom entreprise, prenom contact, nom contact, telephone, adresse, domaine et logo.
- [ ] Verifier que les changements sont sauvegardes dans `company_db`.
- [ ] Verifier que contact, telephone et nom entreprise sont synchronises dans Keycloak.
- [ ] Verifier que le logo s'affiche via URL signee MediaAPI apres rechargement.

## Company / Missions

- [ ] Ajouter dans WSO2 CompanyAPI la ressource `GET /missions`.
- [ ] Charger l'onglet Missions cote Company.
- [ ] Verifier que les missions `BROUILLON`, `PUBLIEE`, `EN_COURS` et `CLOTUREE` de l'entreprise sont visibles.
- [ ] Creer une mission en statut initial `Brouillon`.
- [ ] Creer une mission en statut initial `Publiee`.
- [ ] Verifier qu'une mission publiee apparait cote Freelancer.
- [ ] Modifier une mission brouillon.
- [ ] Modifier une mission publiee.
- [ ] Verifier cote Freelancer que les changements d'une mission publiee sont visibles apres rechargement.
- [ ] Cloturer une mission puis verifier que son statut est visible cote Company.
- [ ] Verifier qu'une mission cloturee ne peut plus etre modifiee.

## Company / Candidatures et notifications

- [ ] Ouvrir l'onglet Candidatures et filtrer par mission via la liste deroulante.
- [ ] Verifier que le profil complet du candidat est visible: photo, nom, prenom, competences, experiences et CV.
- [ ] Cliquer sur `Accepter` pour une candidature.
- [ ] Verifier qu'un message automatique est envoye dans la conversation du freelancer.
- [ ] Verifier cote Freelancer que la cloche affiche: `Vous avez ete accepte pour la mission : [mission] de la societe [entreprise].`
- [ ] Verifier que la mission accepte bien le freelancer assigne.
- [ ] Verifier que le bouton `Payer` apparait uniquement pour une candidature acceptee.
- [ ] Cliquer sur `Payer`.
- [ ] Si le freelancer n'a pas connecte Stripe, verifier le message: `Le freelancer doit d'abord connecter son compte Stripe.`
- [ ] Si le freelancer a connecte Stripe, verifier l'ouverture de la popup paiement Company.
- [ ] Verifier l'affichage du montant, de la commission plateforme et du net freelancer.
- [ ] Confirmer le paiement avec une carte de test Stripe.
- [ ] Verifier que le statut de paiement passe a paye apres confirmation.

## Admin / Dashboard et orchestration

- [ ] Ajouter dans WSO2 AdminAPI la ressource `GET /missions`.
- [ ] Ajouter dans WSO2 AdminAPI la ressource `GET /companies/{id}/missions`.
- [ ] Login Admin avec email/mot de passe correct.
- [ ] Verifier qu'un OTP est envoye par email.
- [ ] Saisir un OTP incorrect et verifier le message d'erreur OTP.
- [ ] Cliquer sur `Generer un nouveau code` et verifier reception d'un nouveau OTP.
- [ ] Saisir un OTP correct et verifier la redirection vers `/admin`.
- [ ] Verifier que le dashboard affiche:
  - total entreprises;
  - entreprises en attente;
  - entreprises validees;
  - entreprises refusees;
  - entreprises suspendues;
  - freelancers;
  - freelancers suspendus;
  - missions creees.
- [ ] Ouvrir la zone Entreprises.
- [ ] Rechercher une entreprise par nom, email, domaine, adresse ou telephone.
- [ ] Filtrer les entreprises par statut.
- [ ] Ouvrir le detail d'une entreprise.
- [ ] Verifier que ses informations generales sont visibles.
- [ ] Verifier que toutes ses missions sont visibles avec titre, description, statut, mode, budget et competences.
- [ ] Accepter une entreprise en statut `Pending`.
- [ ] Refuser une entreprise en statut `Pending`.
- [ ] Verifier que les actions Admin sont ajoutees dans le journal d'audit.
- [ ] Ouvrir la zone Freelancers.
- [ ] Rechercher un freelancer par nom, email ou mot-cle.
- [ ] Filtrer les freelancers actifs ou suspendus.
- [ ] Ouvrir le detail d'un freelancer et verifier son profil.
- [ ] Ouvrir la zone Audits et verifier que les actions recentes sont listees.

### Endpoints Postman via WSO2

Base URL: `http://localhost:8280/api/admin/v1`

```http
GET /missions
Authorization: Bearer <admin_token>
```

```http
GET /companies/{id}/missions
Authorization: Bearer <admin_token>
```

### Ressources WSO2 a ajouter dans AdminAPI

- [ ] `GET /missions`
- [ ] `GET /companies/{id}/missions`
