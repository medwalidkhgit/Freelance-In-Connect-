# Payment Service (Stripe)

Ce module gere les paiements de l'entreprise vers le freelancer avec une commission plateforme de 10%.
Le paiement est declenche quand la mission est marquee comme terminee.

## Fonctionnement
- La plateforme utilise Stripe Connect (Express).
- Le freelancer (ou admin) cree un compte Stripe Connect via l'API.
- Quand la mission est terminee, l'entreprise cree un paiement.
- Stripe encaisse l'entreprise, applique la fee et transfere le net au freelancer.

## Variables d'environnement
- `STRIPE_SECRET_KEY` : cle secrete Stripe.
- `STRIPE_WEBHOOK_SECRET` : secret du webhook Stripe (optionnel pour test local).
- `STRIPE_FEE_PERCENT` : fee plateforme (defaut 10).
- `STRIPE_DEFAULT_CURRENCY` : devise par defaut (defaut `eur`).

## Endpoints
- `POST /api/stripe/accounts` : creer un compte Stripe Connect.
- `POST /api/payments/mission/{missionId}` : creer un paiement apres fin de mission.
- `GET /api/payments/{paymentId}` : recuperer un paiement.
- `POST /api/stripe/webhook` : webhook Stripe (events payment_intent).* 

## Exemple creation compte
```json
POST /api/stripe/accounts
{
  "ownerType": "FREELANCER",
  "ownerId": "freelancer-123",
  "email": "free@example.com"
}
```

## Exemple creation paiement
```json
POST /api/payments/mission/mission-001
{
  "companyId": "company-001",
  "freelancerId": "freelancer-123",
  "amountCents": 150000,
  "currency": "eur"
}
```

## Notes
- Le webhook doit etre expose publiquement pour la production (utiliser Stripe CLI en local).
- Le webhook met a jour le statut du paiement dans la base.

