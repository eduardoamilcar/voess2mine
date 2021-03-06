/**
 * Created by X2382383C on 03/05/2017.
 */
(function() {
    'use strict';

    angular
        .module('voess2App')
        .controller('listavideoscontroller', listavideoscontroller);

    listavideoscontroller.$inject = ['$scope', '$state', 'DataUtils', 'Video', 'ParseLinks', 'AlertService', 'paginationConstants'];

    function listavideoscontroller ($scope, $state, DataUtils, Video, ParseLinks, AlertService, paginationConstants) {
        var vm = this;

        vm.videos = [];
        vm.loadPage = loadPage;
        vm.itemsPerPage = paginationConstants.itemsPerPage;
        vm.page = 0;
        vm.links = {
            last: 0
        };
        vm.predicate = 'id';
        vm.reset = reset;
        vm.reverse = true;
        vm.openFile = DataUtils.openFile;
        vm.byteSize = DataUtils.byteSize;

        loadAll();

        function loadAll () {
            Video.query({
                page: vm.page,
                size: vm.itemsPerPage,
                sort: sort()
            }, onSuccess, onError);
            function sort() {
                var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
                if (vm.predicate !== 'id') {
                    result.push('id');
                }
                return result;
            }

            function onSuccess(data, headers) {
                vm.links = ParseLinks.parse(headers('link'));
                vm.totalItems = headers('X-Total-Count');
                vm.videos = data;
            }

            function onError(error) {
                AlertService.error(error.data.message);
            }
        }
        function reset () {
            vm.page = 0;
            vm.videos = [];
            loadAll();
        }
        function loadPage(page) {
            vm.page = page;
            loadAll();
        }
    }
})();
