(function () {
    'use strict';

    angular
        .module('voess2App')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider.state('settings', {
            parent: 'account',
            url: '/settings',
            data: {
                authorities: ['ROLE_USER'],
                pageTitle: 'global.menu.account.settings'
            },
            views: {
                'content@': {
                    templateUrl: 'app/account/settings/settings.html',
                    controller: 'SettingsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('settings');
                    $translatePartialLoader.addPart('user-management');
                    $translatePartialLoader.addPart('userExt');
                    $translatePartialLoader.addPart('sexGender');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'UserExt', function ($stateParams, UserExt) {
                    return UserExt.getByUser({id: $stateParams.id}).$promise;
                }]
            }
        });
    }
})();
