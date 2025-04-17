# Placeholders PrivateMines

Voici la liste des placeholders disponibles pour PlaceholderAPI avec le plugin PrivateMines.

Utilisation : `%privatemine_<placeholder>%`

| Placeholder                        | Description                                               | Exemple de valeur      |
|-------------------------------------|----------------------------------------------------------|------------------------|
| %privatemine_has_mine%              | Le joueur possède-t-il une mine ?                        | true / false           |
| %privatemine_type%                  | Type de la mine                                          | default, gold, etc.    |
| %privatemine_tier%                  | Palier (niveau) de la mine                               | 1, 2, 3...             |
| %privatemine_size%                  | Taille de la mine                                        | 1, 2, 3...             |
| %privatemine_tax%                   | Taxe de la mine (en %)                                   | 0, 10, 25...           |
| %privatemine_is_open%               | Statut de la mine (ouverte/fermée)                       | Ouverte / Fermée       |
| %privatemine_blocks_mined%          | Nombre de blocs minés dans la mine                       | 1234                   |
| %privatemine_total_blocks%          | Nombre total de blocs dans la mine                       | 10000                  |
| %privatemine_percentage_mined%      | Pourcentage de blocs minés                               | 12                     |
| %privatemine_visits%                | Nombre de visites de la mine                             | 42                     |
| %privatemine_last_reset%            | Date du dernier reset                                    | 01/01/2024 15:30       |
| %privatemine_count%                 | Nombre total de mines sur le serveur                     | 25                     |
| %privatemine_top_<n>_name%          | Nom du joueur à la position n du classement              | Julien_1800            |
| %privatemine_top_<n>_blocks%        | Blocs minés par le joueur à la position n                | 12345                  |
| %privatemine_top_<n>_visits%        | Visites pour la mine à la position n                     | 50                     |
| %privatemine_top_<n>_tier%          | Palier de la mine à la position n                        | 3                      |
| %privatemine_owner%                  | Nom du propriétaire de la mine                                 | Julien_1800               |
| %privatemine_location%               | Coordonnées de la mine (monde, x, y, z)                        | mines, 100, 64, 200       |
| %privatemine_teleport_x%             | Coordonnée X du point de téléportation                         | 105                       |
| %privatemine_teleport_y%             | Coordonnée Y du point de téléportation                         | 65                        |
| %privatemine_teleport_z%             | Coordonnée Z du point de téléportation                         | 205                       |
| %privatemine_teleport_world%         | Monde du point de téléportation                                | mines                     |
| %privatemine_status_color%           | Statut coloré (ex : §aOuverte / §cFermée)                      | §aOuverte                 |
| %privatemine_progress_bar%           | Barre de progression textuelle                                 | [■■■■■■■■■■□□□□□□]        |
| %privatemine_next_reset%             | Pourcentage restant avant le prochain reset automatique        | 23                        |
| %privatemine_time_since_last_reset%  | Temps écoulé depuis le dernier reset (formaté)                 | 2h 15m                    |
| %privatemine_block_ratio%            | Ratio blocs minés / total                                      | 1234/10000                |
| %privatemine_is_full%                | La mine est-elle pleine (prête à reset) ?                      | true / false              |
| %privatemine_visitor_last%           | Dernier visiteur de la mine                                    | Player123                 |
| %privatemine_visitor_count_unique%   | Nombre de visiteurs uniques                                    | 18                        |
| %privatemine_reset_count%            | Nombre total de resets effectués (depuis le dernier reset)     | 12                        |

Exemple d’utilisation dans le chat, scoreboard, hologram, etc. :
```
%privatemine_blocks_mined%
```
Affiche le nombre de blocs minés dans votre mine. 