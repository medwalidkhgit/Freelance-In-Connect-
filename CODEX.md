# Etat de liaison Frontend / Backend - Marketplace SubIT

Date: 2026-06-06

Ce document resume ce qui a ete accompli jusqu'a present dans l'integration du projet Marketplace SubIT.

## Architecture suivie

- Backend compose de microservices Java Spring Boot lances via Docker Compose.
- Frontend React/Vite dans `B2B-Front-End`.
- Authentification geree par Keycloak.
- Validation JWT faite par Spring Security dans chaque microservice.
- Routage HTTP via WSO2 API Gateway.
- Media Service connecte a Azure Blob Storage pour photos, logos et CV.

## WSO2 API Gateway

Les APIs principales ont ete declarees et testees progressivement dans WSO2:

- `AuthAPI`
- `FreelancerAPI`
- `CompanyAPI`
- `MissionAPI`
- `ApplicationAPI`
- `AdminAPI`
- `MediaAPI`
- `PaymentAPI`
- `MessagingAPI` REST

Configuration importante:

- Les ressources metier sont exposees via WSO2 avec security adaptee au contexte.
- Le workaround utilise pour ne pas bloquer Spring Security consiste a faire passer le token backend dans `Authorization`, tout en configurant WSO2 avec un header d'autorisation separe quand necessaire.
- Les endpoints REST principaux repondent via `http://localhost:8280`.

## Authentification et roles

Les roles Keycloak utilises par le frontend sont:

- `ADMIN`
- `COMPANY`
- `FREELANCER`

Le frontend sait maintenant:

- appeler `POST /api/auth/v1/login`;
- stocker `accessToken` et `refreshToken`;
- decoder les roles du JWT;
- rediriger selon le role:
  - `ADMIN` vers `/admin`
  - `COMPANY` vers `/company`
  - `FREELANCER` vers `/freelancer`
- proteger les routes par role.

Correction ecran d'accueil:

- ajout d'une page d'accueil sur `/`;
- la page presente `FIC - Freelance In Connect` et le message de bienvenue de la plateforme;
- le bouton `Entrer` redirige vers `/sign-in`;
- correction CSS globale:
  - les styles Tailwind/theme sont maintenant importes dans `src/index.css`;
  - la page `/` garde le meme positionnement au premier chargement, apres `Entrer`, puis avec le bouton retour navigateur;
  - la page login ne depend plus d'un import CSS local pour initialiser les styles globaux.
- alignement visuel de la page d'accueil:
  - le logo, le texte et le bouton sont maintenant dans un container blanc coherent avec la page login;
  - le bouton `Entrer` reprend le style gradient du bouton `Se connecter`.
  - le bouton `Entrer` a ete elargi pour correspondre visuellement aux boutons principaux de la plateforme.
  - la carte d'accueil a ete compactee pour les petits ecrans et le bouton garde maintenant une largeur minimale stable.
- la page de connexion conserve le formulaire existant, mais le texte `Welcome back` / `Please enter your details...` a ete remplace par un libelle francais:
  - `Veuillez entrer vos informations afin de vous connecter a votre portail.`;
- le bouton de connexion affiche maintenant `Se connecter`.

Correction importante:

- Le frontend attendait initialement `access_token` / `refresh_token`.
- Le backend renvoie `accessToken` / `refreshToken`.
- Le stockage token a ete rendu compatible avec les deux formats.

## Flux d'inscription

Le flux d'inscription a ete corrige pour respecter le comportement attendu:

1. L'utilisateur choisit son type de compte.
2. Il remplit le formulaire Freelancer ou Company.
3. Le compte est cree via `AuthAPI`.
4. Une page de succes est affichee.
5. L'utilisateur clique sur `Se connecter`.
6. Il arrive ensuite dans son espace selon son role.

Changement important:

- L'inscription ne fait plus d'auto-login.
- L'inscription ne fait plus d'upload media.
- Photo, CV et logo sont ajoutes apres connexion dans le profil, car `MediaAPI` est securisee.

## Correction backend Freelancer

Un bug bloquait l'inscription Freelancer.

Cause:

- La base `freelancer_db` contenait deux colonnes liees a Keycloak: `keycloak_id` et `keycloak_user_id`.
- Le service ecrivait dans une colonne pendant que l'autre restait `null`.
- PostgreSQL refusait l'insertion avec une contrainte `NOT NULL`.

Correction:

- L'entite `Freelancer` ecrit maintenant la valeur Keycloak dans les deux colonnes.
- `freelancer-service` a ete recompile, reconstruit et redemarre.

Tests valides:

- `POST /api/auth/v1/freelancer/register`: OK
- login Freelancer: OK
- `GET /api/freelancers/v1/me`: OK

## Espace Freelancer

L'espace Freelancer est branche sur le backend pour:

- afficher le profil reel via `GET /api/freelancers/v1/me`;
- modifier les informations personnelles via `PUT /api/freelancers/v1/me`;
- gerer les competences;
- uploader une photo de profil via `MediaAPI`;
- uploader un CV PDF via `MediaAPI`;
- afficher photo et CV via URLs signees Azure;
- consulter, telecharger, remplacer ou supprimer le CV;
- agrandir la photo de profil;
- supprimer la photo de profil;
- afficher les missions publiees;
- chercher des missions;
- postuler a une mission.
- ouvrir une conversation de messagerie depuis une mission;
- lister les conversations existantes;
- afficher l'historique des messages;
- envoyer un message via `MessagingAPI`.

Corrections UI:

- Le bloc statique `Alex D. / Dev React` a ete supprime du sidebar.
- Le profil affiche maintenant les ressources existantes avant les inputs de remplacement.
- Le fallback image affiche les initiales si l'image ne peut pas etre chargee.
- Le profil n'affiche plus un formulaire vide quand `GET /me` echoue; un etat d'erreur avec bouton `Recharger` est affiche.
- La page Messages affiche une action vers les missions quand aucune conversation n'existe.
- L'input de message indique clairement qu'une conversation doit etre selectionnee avant d'ecrire.
- Correction candidature: l'appel `POST /api/applications/v1/` a ete remplace par `POST /api/applications/v1` sans slash final, car le slash final etait transmis au backend comme `/api/applications/` et Spring retournait `No static resource api/applications`.

## Espace Company

L'espace Company est branche sur le backend pour:

- charger le profil company via `GET /api/companies/v1/me`;
- modifier le profil company;
- uploader le logo apres connexion;
- afficher les missions de la company;
- creer une mission;
- publier une mission;
- demarrer une mission;
- cloturer une mission;
- supprimer une mission;
- voir les candidatures recues;
- changer le statut d'une candidature:
  - `ACCEPTED`
  - `REJECTED`
  - `WAITLISTED`
  - `PENDING`

Corrections Company recentes:

- Le logo entreprise n'est plus affiche directement depuis l'URL Azure stockee en base.
- Le frontend demande maintenant une URL signee via `MediaAPI /read-url` avant d'afficher le logo Company.
- La creation de mission cote Company a ete corrigee cote backend:
  - `company-service` n'envoie plus le Bearer utilisateur dans son appel Feign interne vers `mission-service`;
  - l'appel interne utilise uniquement `X-Internal-Token`, pour conserver `ROLE_INTERNAL` cote MissionAPI;
  - `companyKeycloakId` est transmis dans le payload de mission pour conserver l'identite Keycloak de l'entreprise.
- Le frontend garde la mission creee en memoire locale apres `POST /api/companies/v1/missions`, afin que le brouillon apparaisse immediatement et puisse etre publie.
- `company-service` a ete recompile, reconstruit et redemarre.

## Espace Admin

L'espace Admin est branche sur `AdminAPI` pour:

- charger le profil admin;
- modifier le profil admin;
- afficher toutes les entreprises avec leur statut;
- approuver une entreprise;
- rejeter une entreprise avec raison;
- suspendre une entreprise avec raison;
- supprimer une entreprise avec raison;
- afficher les freelancers;
- suspendre un freelancer;
- supprimer un freelancer;
- afficher les audit logs.

Le compte admin doit etre cree dans Keycloak avec le role `ADMIN`.

Correction coherence Keycloak / bases metier:

- ajout d'un endpoint interne dans `auth-service`: `DELETE /auth/internal/users/{userId}`;
- cet endpoint est protege par `X-Internal-Token`;
- il supprime l'utilisateur Keycloak correspondant au `keycloakId`;
- la suppression Keycloak est idempotente pour les orphelins:
  - si le user n'existe deja plus dans Keycloak, l'operation est consideree comme OK;
- ajout d'un `AuthServiceClient` dans `admin-service`;
- lorsqu'un admin supprime une entreprise:
  - `admin-service` recupere le `keycloakId` de l'entreprise;
  - supprime le user Keycloak via `auth-service`;
  - supprime ensuite la ligne `company_db`;
  - inscrit l'action dans l'audit;
- lorsqu'un admin supprime un freelancer:
  - `admin-service` recupere le `keycloakId` du freelancer;
  - supprime le user Keycloak via `auth-service`;
  - supprime ensuite la ligne `freelancer_db`;
  - inscrit l'action dans l'audit;
- cela evite les incoherences ou Keycloak et les bases metier divergent apres une suppression depuis l'AdminPanel.

Correction AdminAPI:

- les appels `GET /api/admin/v1/` et `PUT /api/admin/v1/` ont ete remplaces par `GET /api/admin/v1` et `PUT /api/admin/v1`;
- raison: le slash final etait transmis au backend comme `/api/admin/`, qui ne matchait pas le `@GetMapping` Spring `/api/admin`;
- cette erreur 404 bloquait le `Promise.all` du dashboard Admin et pouvait laisser les compteurs a `0` meme si `companies/pending` et `freelancers` retournaient bien des donnees.

Correction Gestion Entreprises Admin:

- ajout backend `company-service`: `GET /api/companies/admin` pour retourner toutes les entreprises, pas seulement les `Pending`;
- ajout backend `admin-service`: `GET /api/admin/companies`;
- ajout frontend: service `getCompaniesForAdmin()`;
- l'espace Admin charge maintenant toutes les entreprises via `GET /api/admin/v1/companies`;
- la table affiche `Compte`, `Info`, `Statut`, `Actions`;
- les actions sont maintenant explicites: `Details`, `Valider`, `Rejeter`, `Suspendre`, `Supprimer`;
- les compteurs dashboard distinguent:
  - total entreprises;
  - entreprises en attente;
  - entreprises validees.

Verification:

```bash
npm.cmd run build
mvn package -Dmaven.test.skip=true # company-service
mvn package -Dmaven.test.skip=true # admin-service
docker compose build company-service admin-service
docker compose up -d company-service admin-service
docker compose logs --tail=60 company-service
docker compose logs --tail=60 admin-service
```

Resultat: build frontend OK, compilation Java OK, services Docker redemarres OK.

Action WSO2 requise:

- ajouter dans `AdminAPI` la ressource `GET /companies`;
- sinon le frontend appelera `http://localhost:8280/api/admin/v1/companies` et WSO2 peut retourner `404 Not Found` meme si le backend est corrige.

Correction de robustesse Admin:

- le chargement Admin n'utilise plus un `Promise.all` bloquant pour tous les blocs;
- si `GET /api/admin/v1/companies` echoue parce que WSO2 n'a pas encore la ressource `GET /companies`, le front tente temporairement `GET /api/admin/v1/companies/pending`;
- les freelancers et les audit logs peuvent maintenant se charger meme si la liste complete des entreprises echoue;
- le message affiche precise que `GET /companies` doit etre ajoute dans WSO2 pour voir toutes les entreprises.
- le message global de fallback `Liste complete des entreprises indisponible...` a ete retire pour ne pas polluer les autres onglets Admin.
- la cloche de notification a ete retiree du panel Admin.
- le bouton de deconnexion est maintenant harmonise visuellement entre les espaces Admin, Company et Freelancer:
  - icone `LogOut`;
  - texte `Déconnexion`;
  - fond rouge clair;
  - bordure arrondie identique.
- le bandeau Admin vide n'est plus affiche quand aucun message utile n'est present.
- le fallback Admin des entreprises a ete renforce:
  - si `GET /api/admin/v1/companies` n'est pas encore disponible dans WSO2, le front charge les entreprises `Pending` via `AdminAPI`;
  - il complete temporairement avec les entreprises publiques validees via `CompanyAPI GET /api/companies/v1`;
  - cela evite d'afficher `0 Entreprises` alors que la base contient deja des entreprises validees;
  - `GET /companies` reste a ajouter dans `AdminAPI` WSO2 pour obtenir la liste administrative complete avec emails et statuts reels.

Verification:

```bash
npm.cmd run build
```

Resultat: build frontend OK.

## MediaAPI et Azure Blob Storage

MediaAPI est integree dans le frontend.

Endpoints utilises:

- upload photo freelancer;
- upload logo company;
- upload CV PDF;
- creation d'URL signee via `/api/media/v1/read-url`.

Correction importante:

- Le frontend ne lit plus directement l'URL Azure stable quand le container n'est pas public.
- Il demande une URL signee a MediaAPI avant d'afficher l'image ou d'ouvrir le CV.

## PaymentAPI et Stripe

PaymentAPI est maintenant integree cote frontend.

Endpoints frontend utilises:

- `POST /api/payments/v1/stripe/accounts/freelancers/{freelancerKeycloakId}`
- `POST /api/payments/v1/payments/mission/{missionId}`
- `GET /api/payments/v1/payments/{paymentId}`

Configuration ajoutee:

- ajout de `VITE_STRIPE_PUBLISHABLE_KEY=cle-stripe-a-mettre-plus-tard` dans `.env`;
- ajout de `B2B-Front-End/.env` et `B2B-Front-End/.env.example`, car Vite lit les variables du dossier frontend;
- passage de `STRIPE_DEFAULT_CURRENCY` a `mad` dans `.env` et dans le fallback `docker-compose.yml`;
- la cle secrete Stripe reste cote backend via `STRIPE_SECRET_KEY`;
- le webhook reste cote backend via `STRIPE_WEBHOOK_SECRET`.

Fonctionnalites ajoutees:

- cote Freelancer, le profil contient un bloc `Paiements` avec bouton `Connecter mon compte Stripe`;
- ce bouton cree un compte Stripe Connect pour le freelancer via PaymentAPI et redirige vers l'onboarding Stripe;
- cote Company, les candidatures acceptees affichent un bouton `Payer`;
- le paiement utilise `company.keycloakId`, `application.freelancerKeycloakId`, le budget de la mission et la devise `mad`;
- le frontend ouvre maintenant une modale Stripe avec saisie carte via Stripe.js;
- si un paiement est initialise mais pas termine, le bouton devient `Finaliser paiement` et rouvre la modale existante;
- le frontend affiche le statut du paiement: `a finaliser`, `confirme`, `echoue`, etc.;
- le montant, la commission plateforme et le net freelancer sont affiches en MAD dans la modale Company.

Point important:

- le paiement reel Stripe ne peut etre teste qu'apres remplacement des placeholders Stripe par de vraies cles de test;
- le paiement Company exige que le freelancer ait deja configure son compte Stripe Connect;
- les anciennes candidatures sans `freelancerKeycloakId` ne peuvent pas initialiser un paiement proprement.

Verification:

```bash
npm.cmd run build
```

Resultat: build frontend OK apres integration de la modale Stripe Company et du bloc Stripe Freelancer.

Correction bouton Stripe Freelancer:

- ajout d'un message local dans le bloc `Paiements`;
- au clic, le bloc affiche maintenant `Connexion a Stripe en cours...`;
- si WSO2/CORS bloque l'appel PaymentAPI, le message indique de verifier `OPTIONS /stripe/accounts/freelancers/{freelancerId}`;
- si Stripe ne retourne pas de lien d'onboarding, le message est affiche directement dans la carte;
- le build frontend reste OK.

## MessagingAPI

La liaison MessagingAPI a ete demarree cote frontend et backend.

Endpoints frontend utilises:

- `GET /api/messaging/v1/conversations`
- `POST /api/messaging/v1/conversations`
- `GET /api/messaging/v1/messages/conversations/{conversationId}`
- `POST /api/messaging/v1/messages`
- WebSocket SockJS/STOMP via `/api/messaging/v1/ws`

Changements frontend:

- ajout des dependances `@stomp/stompjs` et `sockjs-client`;
- ajout d'un helper `MessagingSocket`;
- ajout des services REST `listConversations`, `createConversation`, `getConversationMessages`, `sendChatMessage`;
- remplacement des messages mockes de l'espace Freelancer par les appels backend;
- creation automatique d'une conversation depuis une mission avec route `new-{missionId}`;
- affichage de l'historique et reception temps reel quand le WebSocket est disponible.

Changement backend:

- ajout de `POST /api/messages` dans `messaging-service`.
- l'envoi REST sauvegarde le message puis publie aussi sur `/topic/conversations/{conversationId}`.
- ajout de `freelancerId` et `freelancerKeycloakId` dans la reponse `ApplicationAPI`, afin que Company puisse ouvrir une conversation depuis une candidature.
- ajout de la colonne `freelancer_id` dans `application-service` pour les nouvelles candidatures.

Raison:

- le WebSocket reste utile pour la reception temps reel;
- l'envoi par REST est plus robuste derriere WSO2, car Spring Security valide directement le JWT sur l'appel HTTP.

Verification:

- `messaging-service` compile avec Maven Docker.
- `messaging-service` a ete reconstruit et redemarre via Docker Compose.
- le service demarre correctement sur `8087` et se connecte a MongoDB.
- le broker WebSocket Spring demarre correctement.
- `application-service` compile avec Maven Docker.
- `application-service` a ete reconstruit et redemarre via Docker Compose.
- Hibernate a ajoute la colonne `freelancer_id` dans la table `applications`.
- correction Vite ajoutee pour `sockjs-client`: `global` est mappe vers `globalThis` afin d'eviter l'erreur navigateur `global is not defined`.
- correction UX Freelancer: le profil ne tombe plus sur un faux etat vide si le backend ne repond pas, et la messagerie guide l'utilisateur vers les missions quand aucune conversation n'existe.
- correction route historique messages: `messaging-service` accepte maintenant les deux chemins `GET /api/messages/conversation/{conversationId}` et `GET /api/messages/conversations/{conversationId}`;
- le frontend utilise la forme plurielle `/messages/conversations/{conversationId}` pour correspondre a la declaration WSO2 actuelle.

## Messagerie Company

L'espace Company dispose maintenant d'un onglet `Messages`.

Fonctionnalites ajoutees:

- chargement des conversations visibles via `MessagingAPI`;
- affichage du freelancer, de la mission et de l'historique;
- ouverture d'une conversation depuis une candidature;
- envoi de messages via `POST /api/messaging/v1/messages`;
- reception temps reel via WebSocket quand disponible;
- affichage des candidatures non encore liees a une conversation.

Point important:

- les anciennes candidatures creees avant l'ajout de `freelancer_id` peuvent ne pas permettre l'ouverture directe d'une conversation cote Company;
- les nouvelles candidatures contiennent `freelancerId` et `freelancerKeycloakId`, donc le flux Company -> Freelancer est exploitable.

## Builds et verification

Le build frontend React/Vite a ete lance plusieurs fois et passe correctement:

```bash
npm.cmd run build
```

Les services Docker concernes ont ete reconstruits/redemarres quand necessaire, notamment:

```bash
docker compose build freelancer-service
docker compose up -d freelancer-service
```

## Etat actuel

La liaison est fonctionnelle pour:

- inscription Freelancer;
- inscription Company;
- login;
- routage par role;
- profil Freelancer;
- media Freelancer;
- profil Company;
- missions Company;
- candidatures;
- espace Admin;
- validation administrative.
- debut de messagerie Freelancer via MessagingAPI.
- debut de messagerie Company via MessagingAPI.
- debut d'integration PaymentAPI:
  - onboarding Stripe freelancer;
  - initialisation paiement company sur candidature acceptee.

## Corrections Freelancer UI - 06/06/2026

Corrections ajoutees cote front Freelancer:

- suppression de la navigation basse redondante dans `FreelanceHub`, car la navigation principale est deja geree par la sidebar gauche;
- correction du panneau detail mission pour garder visibles les actions:
  - `Ecrire a l'entreprise`;
  - `Postuler directement`;
- correction de la messagerie Freelancer:
  - selection automatique d'une conversation existante quand on arrive sur `Mes messages`;
  - affichage desktop en deux zones fixes: liste des conversations + panneau de chat;
  - activation correcte du champ d'ecriture quand une conversation est selectionnee;
  - recuperation d'URL signee MediaAPI pour afficher le logo de l'entreprise dans les conversations quand il existe.
- correction WSO2/CORS pour MessagingAPI:
  - les appels MessagingAPI utilisent seulement `Authorization`;
  - le header custom `wso2Authorization` a ete retire du front pour s'aligner avec la securite WSO2 desactivee sur les ressources;
  - `OPTIONS /conversations` est valide quand le preflight demande uniquement `authorization`;
  - WSO2 retourne maintenant les headers CORS attendus pour `GET /conversations`.
- alignement des missions affichees au Freelancer:
  - les budgets sont affiches en MAD au lieu de EUR;
  - les cartes missions affichent le nom de l'entreprise au lieu de `Company #id`;
  - les cartes missions et le panneau detail affichent le logo entreprise quand il est disponible;
  - les logos entreprise sont recuperes via URL signee MediaAPI.
- amelioration de l'en-tete Freelancer:
  - suppression de la barre `Recherche rapide...` sur les pages Freelancer;
  - la cloche ouvre maintenant un menu de notifications;
  - les notifications sont derivees des conversations et des missions publiees;
  - exemples affiches: message recu d'une entreprise, nouvelle mission lancee par une entreprise.
- correction du menu notifications Freelancer:
  - le menu est maintenant affiche au-dessus du contenu avec une position fixe;
  - les notifications apparaissent en liste deroulante;
  - chaque item est type visuellement: `Message` ou `Mission publiee`.

Verification:

```bash
npm.cmd run build
```

Resultat: build frontend OK.

## Correction Company Candidatures - 06/06/2026

Correction backend appliquee pour l'action `Accepter` cote Company:

- le `403` venait de l'appel interservice `application-service -> mission-service`;
- `mission-service` exige `ROLE_INTERNAL` sur `PUT /api/missions/{id}/assign-freelancer`;
- le filtre `InternalServiceTokenFilter` de `mission-service` donne maintenant priorite a `X-Internal-Token` quand il est valide;
- le filtre interne est execute apres le filtre Bearer JWT pour eviter que le token utilisateur masque le role interne;
- `mission-service` a ete recompile, reconstruit et redemarre.

Verification:

```bash
mvn package -Dmaven.test.skip=true
docker compose build mission-service
docker compose up -d mission-service
docker compose logs --tail=60 mission-service
```

## Correction coherence Keycloak / bases metier - 07/06/2026

Correction backend appliquee pour eviter les incoherences entre Keycloak et les bases `company` / `freelancer`:

- Keycloak reste la source des identites, les bases Company/Freelancer restent les sources des profils metier;
- ajout d'un endpoint interne AuthService: `DELETE /auth/internal/users/{userId}`;
- cet endpoint est protege par `X-Internal-Token` et n'est pas destine a WSO2;
- la suppression Keycloak est maintenant idempotente: `204` et `404` sont acceptes;
- AdminService supprime maintenant dans cet ordre:
  1. recuperation du profil Company/Freelancer;
  2. suppression du compte Keycloak via AuthService;
  3. suppression de la ligne metier via CompanyService/FreelancerService;
  4. ecriture de l'audit log.

Impact:

- si un utilisateur a deja ete supprime manuellement dans Keycloak, Admin peut quand meme supprimer la ligne orpheline en base metier;
- la bonne pratique devient: ne plus supprimer directement dans Keycloak, supprimer depuis AdminPanel/AdminAPI.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... admin-service ... mvn package "-Dmaven.test.skip=true"
docker compose build auth-service admin-service
docker compose up -d auth-service admin-service
docker compose logs --tail=80 auth-service admin-service
```

Resultat: compilation OK, images reconstruites, `auth-service` et `admin-service` redemarres correctement.

## Auth - Blocage Company Pending - 07/06/2026

Correction appliquee sur le socle inscription/login:

- les messages d'inscription AuthService sont nettoyes et coherents:
  - Freelancer: `Inscription reussie`;
  - Company: `Inscription reussie. En attente de confirmation par l'administrateur.`;
- apres login Keycloak, AuthService lit le role dans le token;
- si le role est `COMPANY`, AuthService appelle `company-service /api/companies/me`;
- si le statut company n'est pas `Validated`, AuthService refuse la session et revoque le refresh token;
- pour une company `Pending`, le message retourne est:
  `Compte non verifie par l'administrateur, veuillez patienter.`
- cote frontend:
  - les ecrans succes Freelancer et Company redirigent automatiquement vers `/sign-in`;
  - le bouton manuel `Se connecter` reste disponible;
  - les libelles login visibles sont alignes en francais.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker compose build auth-service
docker compose up -d auth-service
docker compose logs --tail=60 auth-service
docker compose ps auth-service
npm.cmd run build
```

Resultat: compilation backend OK, image reconstruite, `auth-service` redemarre correctement, build frontend OK.

## Inscription avec fichiers MediaAPI - 07/06/2026

Correction appliquee pour aligner l'inscription avec le flux cible:

- les formulaires d'inscription Freelancer acceptent maintenant:
  - photo de profil: `image/png`, `image/jpeg`, `image/webp`;
  - CV: `application/pdf`;
- le formulaire d'inscription Company accepte maintenant:
  - logo entreprise: `image/png`, `image/jpeg`, `image/webp`;
- les fichiers sont uploades via MediaAPI avant l'appel register;
- les URLs Azure retournees sont transmises a AuthAPI;
- AuthAPI transmet `cvUrl` / `pfpUrl` au profil Freelancer;
- AuthAPI transmet le logo Company via `pfpUrl`;
- CompanyService stocke maintenant `pfpUrl` des la creation du compte `Pending`;
- MediaAPI autorise les uploads d'inscription sans JWT, tout en gardant les validations type/taille.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... company-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... media-service ... mvn package "-Dmaven.test.skip=true"
npm.cmd run build
docker compose build auth-service company-service media-service
docker compose up -d auth-service company-service media-service
docker compose logs --tail=80 auth-service company-service media-service
```

Resultat: builds backend OK, build frontend OK, services redemarres correctement.

## Mot de passe oublie avec OTP - 07/06/2026

Correction appliquee pour aligner le reset password avec le flux cible:

- ajout des endpoints publics AuthAPI:
  - `POST /auth/password/forgot`;
  - `POST /auth/password/verify-otp`;
  - `POST /auth/password/reset`;
- AuthService verifie maintenant l'existence de l'email dans les bases metier avant de generer un OTP;
- ajout d'endpoints internes, non exposes WSO2:
  - `GET /api/companies/internal/email-exists?email=...`;
  - `GET /api/freelances/internal/email-exists?email=...`;
- ces endpoints internes sont proteges par `X-Internal-Token` / `ROLE_INTERNAL`;
- ajout d'un store OTP en memoire dans AuthService, avec expiration configuree par `OTP_TTL_MINUTES` (10 minutes par defaut);
- ajout de la configuration SMTP:
  - `SMTP_ENABLED`;
  - `SMTP_HOST`;
  - `SMTP_PORT`;
  - `SMTP_USERNAME`;
  - `SMTP_PASSWORD`;
  - `SMTP_FROM`;
  - `SMTP_AUTH`;
  - `SMTP_STARTTLS`;
- si `SMTP_ENABLED=false`, le code OTP est affiche dans les logs `auth-service` pour faciliter les tests locaux;
- la reinitialisation met a jour le mot de passe dans Keycloak, sans stocker le mot de passe dans `company_db` ou `freelancer_db`;
- ajout de la page front `/forgot-password`:
  - saisie email;
  - validation OTP;
  - nouveau mot de passe + confirmation;
  - retour vers `/sign-in`.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... company-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... freelancer-service ... mvn package "-Dmaven.test.skip=true"
npm.cmd run build
docker compose build auth-service company-service freelancer-service
docker compose up -d auth-service company-service freelancer-service
docker compose logs --tail=80 auth-service company-service freelancer-service
```

Resultat: builds backend OK, build frontend OK, services redemarres correctement.

## Connexion avec OTP - 07/06/2026

Correction appliquee pour ajouter un second facteur OTP apres verification email/mot de passe:

- `POST /auth/login` valide d'abord les identifiants via Keycloak;
- pour une entreprise, le statut Company est verifie avant envoi OTP:
  - si `Pending`, le login reste bloque avec le message de compte non verifie;
  - si `Validated`, le flux OTP demarre;
- les tokens Keycloak ne sont plus envoyes directement au frontend apres login;
- `auth-service` conserve temporairement les tokens en memoire avec un `challengeId`;
- un OTP est envoye par SMTP a l'adresse du compte;
- ajout des endpoints publics:
  - `POST /auth/login/verify-otp`;
  - `POST /auth/login/resend-otp`;
- si l'OTP est correct, `verify-otp` retourne les tokens et le frontend connecte l'utilisateur;
- si l'OTP est incorrect, le message `Code OTP invalide.` est affiche;
- si le code expire, l'utilisateur doit recommencer la connexion;
- le frontend login affiche maintenant une deuxieme etape `Code OTP`;
- ajout d'un bouton `Generer un nouveau code`.

Verification:

```bash
npm.cmd run build
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker compose build auth-service
docker compose up -d auth-service
docker compose logs --tail=60 auth-service
```

Resultat: build frontend OK, compilation backend OK, `auth-service` redemarre correctement.

Action WSO2 requise:

- ajouter dans `AuthAPI`:
  - `POST /login/verify-otp`;
  - `POST /login/resend-otp`.

## Freelancer Missions - Recherche et filtres - 07/06/2026

Correction appliquee pour completer la recherche de missions cote Freelancer:

- `MissionAPI GET /search` accepte maintenant:
  - `workMode`;
  - `skill`;
  - `minBudget`;
  - `maxBudget`;
- les filtres backend sont combines au lieu d'etre exclusifs;
- le frontend Freelancer ajoute les presets budget:
  - inferieur a 5 000 MAD;
  - 5 000 - 10 000 MAD;
  - 10 000 - 25 000 MAD;
  - superieur a 25 000 MAD;
  - intervalle personnalise;
- la recherche mot-cle cote frontend couvre maintenant:
  - titre;
  - description;
  - competences/technologies;
  - nom d'entreprise;
  - domaine d'entreprise;
- les filtres peuvent etre combines: mode + competence + budget + mot-cle.

Verification:

```bash
npm.cmd run build
docker run --rm ... mission-service ... mvn package "-Dmaven.test.skip=true"
docker compose build mission-service
docker compose up -d mission-service
docker compose logs --tail=80 mission-service
```

Resultat: build frontend OK, compilation backend OK, `mission-service` redemarre correctement.

WSO2:

- aucune nouvelle ressource requise si `GET /search` existe deja dans `MissionAPI`;
- les nouveaux filtres passent par query params.

## Cloture Espace Freelancer - 07/06/2026

Corrections appliquees pour terminer les points restants de l'espace Freelancer:

- modification du profil Freelancer synchronisee avec Keycloak:
  - nom;
  - prenom;
  - telephone;
- ajout d'un endpoint interne AuthService:
  - `PUT /auth/internal/users/{userId}/profile`;
  - protege par `X-Internal-Token`;
  - non destine a WSO2;
- `freelancer-service` appelle cet endpoint apres sauvegarde du profil metier;
- le DTO profil Freelancer expose maintenant les informations utiles cote Company:
  - identifiant;
  - Keycloak user id;
  - email;
  - telephone;
  - photo;
  - CV;
  - competences;
  - experiences;
- quand un Freelancer postule, `application-service` cree automatiquement une conversation dans `messaging-service`;
- si une lettre de motivation est fournie, elle est envoyee comme message lie a la mission;
- sans lettre, un message de candidature par defaut est envoye;
- `application-service` expose aussi un controle d'acces interne au flux Company:
  - une entreprise ne peut voir le profil complet que si une candidature existe pour ses missions;
  - l'endpoint direct `GET /api/freelances/{id}` refuse les autres entreprises;
  - la liste globale des profils freelancers est reservee a l'admin;
- cote Company, l'onglet Candidatures charge le profil complet du Freelancer et affiche:
  - photo;
  - nom;
  - email;
  - telephone;
  - resume;
  - competences;
  - experiences;
  - lien CV via URL signee MediaAPI.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... freelancer-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... application-service ... mvn package "-Dmaven.test.skip=true"
npm.cmd run build
docker compose build auth-service freelancer-service application-service
docker compose up -d auth-service freelancer-service application-service
docker compose logs --tail=120 auth-service freelancer-service application-service
```

Resultat: compilations backend OK, build frontend OK, images reconstruites, services redemarres correctement.

## Payment Stripe Freelancer - 07/06/2026

Diagnostic du bouton `Connecter mon compte Stripe`:

- `payment-service` etait bien demarre;
- le clic arrivait jusqu'au backend;
- la variable `STRIPE_SECRET_KEY` contenait encore le placeholder `sk_test_xxx`;
- l'appel Stripe echouait donc et le backend retournait une erreur;
- le frontend n'affichait pas le champ JSON `error`, ce qui donnait seulement `HTTP 502`.

Corrections appliquees:

- `apiFetch` lit maintenant aussi le champ `error` dans les reponses JSON d'erreur;
- `payment-service` detecte explicitement `STRIPE_SECRET_KEY=sk_test_xxx`;
- dans ce cas, le service retourne un message clair:
  `Stripe n'est pas configure: renseignez STRIPE_SECRET_KEY avec une vraie cle de test Stripe.`;
- ajout des URLs configurables pour le retour onboarding Stripe:
  - `STRIPE_ACCOUNT_LINK_REFRESH_URL`;
  - `STRIPE_ACCOUNT_LINK_RETURN_URL`;
  - valeurs par defaut: `http://localhost:5173/freelancer`.

Action requise pour tester le paiement reel:

- creer ou utiliser des cles Stripe de test;
- mettre une vraie `STRIPE_SECRET_KEY` dans `.env`;
- mettre `VITE_STRIPE_PUBLISHABLE_KEY` dans `B2B-Front-End/.env`;
- redemarrer `payment-service`.

Verification:

```bash
npm.cmd run build
docker run --rm ... payment-service ... mvn package "-Dmaven.test.skip=true"
docker compose build payment-service
docker compose up -d payment-service
docker compose logs --tail=80 payment-service
```

Resultat: build frontend OK, compilation payment-service OK, image reconstruite, service redemarre correctement.

## Reset donnees metier et paiement - 07/06/2026

Nettoyage effectue pour repartir sur des donnees coherentes:

- `freelancer_db` vide:
  - `freelancer`;
  - `freelancer_skills`;
  - `freelancer_experiences`;
  - `freelancer_projects`;
- `company_db` vide:
  - `companies`;
- `mission_db` vide:
  - `mission`;
  - `mission_required_skills`;
- `application_db` vide:
  - `applications`;
- `payment_db` vide:
  - `payments`;
  - `stripe_accounts`;
  - `stripe_webhook_events`;
- suppression du seed automatique dans `mission-service`;
- suppression du fichier frontend de missions mock:
  - `B2B-Front-End/src/freelancer/data/mockMissions.ts`.

Point Keycloak:

- les utilisateurs Keycloak existants ne sont pas supprimes par ces resets SQL;
- pour reutiliser les memes emails, supprimer aussi les users Keycloak correspondants ou utiliser de nouveaux emails.

## Payment Stripe Company -> Freelancer - 07/06/2026

Flux retenu:

- l'admin n'a pas de paiement;
- le freelancer connecte son compte Stripe Express depuis son profil;
- la company paie une candidature acceptee depuis son espace;
- le paiement est cree par `payment-service` avec `PaymentIntent`;
- la commission plateforme est appliquee via `application_fee_amount`;
- le montant net est transfere vers le compte Stripe Connect du freelancer via `transfer_data.destination`.

Corrections appliquees:

- si un freelancer a deja un compte Stripe local, un nouveau lien onboarding est genere au lieu de bloquer;
- si une company tente de payer un freelancer sans compte Stripe connecte, le backend retourne:
  `Le freelancer doit d'abord connecter son compte Stripe.`;
- si `STRIPE_SECRET_KEY` est absent ou placeholder, le backend retourne un message clair;
- les erreurs `ResponseStatusException` de `payment-service` sont renvoyees au format JSON `{ error: ... }`;
- `payment-service` a ete reconstruit et redemarre avec:
  - `STRIPE_FEE_PERCENT=10`;
  - `STRIPE_DEFAULT_CURRENCY=mad`;
  - `STRIPE_ACCOUNT_LINK_RETURN_URL=http://localhost:5175/freelancer`;
  - `STRIPE_ACCOUNT_LINK_REFRESH_URL=http://localhost:5175/freelancer`.

Point restant pour paiement carte cote frontend:

- renseigner `VITE_STRIPE_PUBLISHABLE_KEY` dans `B2B-Front-End/.env`;
- redemarrer le serveur Vite apres modification de cette variable.

## Espace Company - Profil, Missions, Candidatures - 07/06/2026

Corrections appliquees pour aligner l'espace Company avec les specifications:

- profil Company:
  - ajout des champs de contact dans le formulaire profil;
  - sauvegarde nom entreprise, contact, telephone, adresse, domaine et logo dans `company_db`;
  - ajout d'un client interne `company-service -> auth-service`;
  - synchronisation Keycloak apres modification du profil:
    - firstName = prenom contact;
    - lastName = nom contact;
    - attribut `phone`;
    - attribut `companyName`;
- missions Company:
  - l'endpoint public `MissionAPI GET /company/{companyId}` reste limite aux missions publiees;
  - ajout d'un endpoint interne MissionAPI `GET /api/missions/company/{companyId}/all`;
  - ajout de `CompanyAPI GET /missions` pour que l'entreprise voie toutes ses missions;
  - le frontend Company utilise maintenant `GET /api/companies/v1/missions`;
  - la vue recapitulative affiche donc brouillons, publiees, en cours et cloturees;
  - une mission peut etre creee en `Brouillon` ou directement `Publiee`;
  - une mission brouillon, publiee ou en cours peut etre modifiee;
  - une mission cloturee ne peut plus etre modifiee;
  - ajout d'une edition inline des missions cote Company;
- candidatures:
  - apres `Accepter`, `application-service` assigne le freelancer a la mission;
  - un message automatique est envoye dans la messagerie:
    `Vous avez ete accepte pour la mission : [mission] de la societe [entreprise].`;
  - la cloche Freelancer lit maintenant les candidatures acceptees et affiche une notification dediee.

Verification:

```bash
docker run --rm ... auth-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... company-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... mission-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... application-service ... mvn package "-Dmaven.test.skip=true"
npm.cmd run build
docker compose build auth-service company-service mission-service application-service
docker compose up -d auth-service company-service mission-service application-service
docker compose ps auth-service company-service mission-service application-service
```

Resultat: compilations backend OK, build frontend OK, images reconstruites, services redemarres et `Up`.

Action WSO2 requise:

- ajouter dans `CompanyAPI` la ressource `GET /missions`;
- backend cible: `GET /api/companies/missions`;
- cette ressource est authentifiee role `COMPANY`.

## Espace Admin - Dashboard, recherches et missions - 07/06/2026

Corrections appliquees pour aligner l'espace Admin avec les specifications:

- le login Admin utilise deja le meme flux OTP que Company et Freelancer:
  - login email/mot de passe;
  - envoi du code OTP;
  - verification OTP;
  - generation possible d'un nouveau code;
  - redirection vers `/admin` apres validation;
- le dashboard Admin affiche maintenant les indicateurs principaux:
  - total entreprises;
  - entreprises en attente;
  - entreprises validees;
  - entreprises refusees;
  - entreprises suspendues;
  - freelancers;
  - freelancers suspendus;
  - missions creees;
- ajout d'un endpoint interne MissionAPI:
  - `GET /api/missions/admin/all`;
  - protege par le role interne `INTERNAL`;
  - retourne toutes les missions pour le dashboard Admin;
- ajout dans AdminAPI:
  - `GET /api/admin/missions`;
  - `GET /api/admin/companies/{id}/missions`;
- `admin-service` recupere les missions via `mission-service` avec `X-Internal-Token`;
- la zone Entreprises permet maintenant:
  - recherche par nom, email, domaine, adresse ou telephone;
  - filtre par statut;
  - consultation du detail entreprise;
  - consultation des missions de l'entreprise avec titre, description, statut, mode, budget et competences;
- la zone Freelancers permet maintenant:
  - recherche par nom, email ou identifiant;
  - filtre actifs/suspendus;
  - consultation du detail profil;
- le journal d'audit reste branche sur les actions Admin existantes:
  - acceptation;
  - refus;
  - suspension;
  - suppression.

Verification:

```bash
docker run --rm ... mission-service ... mvn package "-Dmaven.test.skip=true"
docker run --rm ... admin-service ... mvn package "-Dmaven.test.skip=true"
npm.cmd run build
docker compose build mission-service admin-service
docker compose up -d mission-service admin-service
docker compose ps mission-service admin-service
docker compose logs --tail=120 mission-service
docker compose logs --tail=120 admin-service
```

Resultat: compilations backend OK, build frontend OK, images reconstruites, services redemarres et `Up`.

Action WSO2 requise:

- ajouter dans `AdminAPI` la ressource `GET /missions`;
- backend cible: `GET /api/admin/missions`;
- ajouter dans `AdminAPI` la ressource `GET /companies/{id}/missions`;
- backend cible: `GET /api/admin/companies/{id}/missions`;
- ces ressources sont authentifiees role `ADMIN`.

## Prochaines etapes recommandees

1. Tester le flux complet Company:
   - inscription company;
   - validation admin;
   - login company;
   - creation et publication d'une mission.

2. Tester le flux complet Freelancer:
   - login freelancer;
   - consultation des missions publiees;
   - candidature a une mission.

3. Tester le traitement des candidatures cote Company:
   - voir candidature;
   - accepter;
   - rejeter;
   - waitlist.

4. Finaliser la messagerie:
   - confirmer la declaration WSO2 de `MessagingAPI`;
   - tester `GET /conversations`, `POST /conversations`, `GET /messages/conversation/{id}`, `POST /messages`;
   - verifier le WebSocket `/ws` via WSO2;
   - tester une nouvelle candidature pour verifier que `freelancerId` est bien present;
   - verifier l'echange de messages dans les deux sens Company/Freelancer.

5. Nettoyer les derniers mocks et textes:
   - verifier les libelles;
   - corriger les textes mal encodes;
   - finaliser l'experience responsive.

6. Tester PaymentAPI:
   - remplacer les placeholders Stripe par des cles de test;
   - configurer Stripe Connect cote freelancer;
   - accepter une candidature cote Company;
   - initialiser un paiement;
   - verifier le statut via PaymentAPI et webhook Stripe.
