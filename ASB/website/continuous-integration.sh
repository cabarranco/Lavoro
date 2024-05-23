# pay attention: this script should be run just on server

composer install --optimize-autoloader --no-dev

# cache management
php artisan cache:clear
php artisan route:cache
php artisan view:clear
php artisan config:cache

# migrations
php artisan migrate

# front configuration
npm i
#npm run prod

# job management (should be the last command)
#php artisan queue:restart
