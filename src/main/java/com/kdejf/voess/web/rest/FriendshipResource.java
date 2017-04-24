package com.kdejf.voess.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.kdejf.voess.domain.Friendship;
import com.kdejf.voess.domain.User;

import com.kdejf.voess.repository.FriendshipRepository;
import com.kdejf.voess.repository.UserRepository;
import com.kdejf.voess.security.SecurityUtils;
import com.kdejf.voess.web.rest.util.HeaderUtil;
import com.kdejf.voess.web.rest.util.PaginationUtil;

import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.time.ZonedDateTime;

/**
 * REST controller for managing Friendship.
 */
@RestController
@RequestMapping("/api")
public class FriendshipResource {

    private final Logger log = LoggerFactory.getLogger(FriendshipResource.class);

    @Inject
    private FriendshipRepository friendshipRepository;

    @Inject
    private UserRepository userRepo;

    /**
     * POST  /friendships : Create a new friendship.
     *
     * @param friendship the friendship to create
     * @return the ResponseEntity with status 201 (Created) and with body the new friendship, or with status 400 (Bad Request) if the friendship has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/friendships")
    @Timed
    public ResponseEntity<Friendship> createFriendship(@RequestBody Friendship friendship) throws URISyntaxException {
        log.debug("REST request to save Friendship : {}", friendship);
        if (friendship.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("friendship", "idexists", "A new friendship cannot already have an ID")).body(null);
        }
        Friendship result = friendshipRepository.save(friendship);
        return ResponseEntity.created(new URI("/api/friendships/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("friendship", result.getId().toString()))
            .body(result);
    }


    @PostMapping("/new-frienship-to/{id}")
    @Timed
    public ResponseEntity<Friendship> createNewFriendship(@PathVariable Long id) throws URISyntaxException {
        User user1 = userRepo.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get();
        User user2 = userRepo.findOne(id);
        if (user2 == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("friendship", "nouserexist", "A new friendship cannot already exist")).body(null);
        }
        Friendship exist = friendshipRepository.findByFrienshipFromIdAndFrienshipToId(user1.getId(), user2.getId());
        ZonedDateTime today = ZonedDateTime.now();


        if (exist != null && exist.getFinishDateTime() == null) {
            log.debug("REST request to save Friendship EXIST : {}", exist);
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("friendship", "exists", "A new friendship cannot already exist")).body(null);
        } else if (exist != null && exist.getFinishDateTime() != null) {
            log.debug("REST request to save Friendship EXIST but UPDATE: {}", exist);
            exist.setFinishDateTime(null);
            exist.setStartDateTime(today);
            friendshipRepository.save(exist);
            log.debug("REST request to save Friendship AFTER UPDATE: {}", exist);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("friendship", exist.getId().toString()))
                .body(exist);
        } else if (user1.getId() == user2.getId()) {
            log.debug("Recursive friendship");
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("friendship", "recursivefriendship", "Recursive friendship")).body(null);
        } else {
            Friendship friendship = new Friendship();
            friendship.setFrienshipFrom(user1);
            friendship.setFrienshipTo(user2);
            friendship.setStartDateTime(today);
            exist = friendshipRepository.save(friendship);
            log.debug("REST request to save Friendship CREATED : {}", exist);
            return ResponseEntity.created(new URI("/api/friendships/" + exist.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("friendship", exist.getId().toString()))
                .body(exist);

        }

    }


    /**
     * PUT  /friendships : Updates an existing friendship.
     *
     * @param friendship the friendship to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated friendship,
     * or with status 400 (Bad Request) if the friendship is not valid,
     * or with status 500 (Internal Server Error) if the friendship couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/friendships")
    @Timed
    public ResponseEntity<Friendship> updateFriendship(@RequestBody Friendship friendship) throws URISyntaxException {
        log.debug("REST request to update Friendship : {}", friendship);
        if (friendship.getId() == null) {
            return createFriendship(friendship);
        }
        Friendship result = friendshipRepository.save(friendship);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("friendship", friendship.getId().toString()))
            .body(result);
    }



    @PutMapping("/delete-frienship-to/{id}")
    @Timed
    public ResponseEntity<Friendship> finishFriendship(@PathVariable Long id) throws URISyntaxException {
        User user1 = userRepo.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get();
        User user2 = userRepo.findOne(id);
         if (user2 == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createEntityUpdateAlert("friendship", "nouserexist")).body(null);
        }
        ZonedDateTime today = ZonedDateTime.now();
        Friendship friendship = friendshipRepository.findByFrienshipFromIdAndFrienshipToIdAndFinishDateTimeIsNotdefined(user1.getId(), user2.getId());
        if (friendship==null){
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("friendship", "finished", "A new friendship is already finished")).body(null);
        }
        Friendship result=friendship;
        result.setFinishDateTime(today);
        friendshipRepository.save(result);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("friendship", friendship.getId().toString()))
            .body(result);
    }


    /**
     * GET  /friendships : get all the friendships.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of friendships in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/friendships")
    @Timed
    public ResponseEntity<List<Friendship>> getAllFriendships(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Friendships");
        Page<Friendship> page = friendshipRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/friendships");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /friendships/:id : get the "id" friendship.
     *
     * @param id the id of the friendship to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the friendship, or with status 404 (Not Found)
     */
    @GetMapping("/friendships/{id}")
    @Timed
    public ResponseEntity<Friendship> getFriendship(@PathVariable Long id) {
        log.debug("REST request to get Friendship : {}", id);
        Friendship friendship = friendshipRepository.findOne(id);
        return Optional.ofNullable(friendship)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/friendships/betwen-user-and/{id}")
    @Timed
    public ResponseEntity<Friendship> getFriendshipBetwenUsers(@PathVariable Long id) {
       log.debug("REST request to get Friendship betwen user and : {}", id);
        User user1 = userRepo.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get();
        User user2=userRepo.findOne(id);
        if (user2==null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createAlert("friendship", "nouserexist")).body(null);
        }
        Friendship friendship = friendshipRepository.findByFrienshipFromIdAndFrienshipToIdAndFinishDateTimeIsNotdefined(user1.getId(), user2.getId());
        return Optional.ofNullable(friendship)
           .map(result -> new ResponseEntity<>(
               result,
                HttpStatus.OK))
           .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /friendships/:id : delete the "id" friendship.
     *
     * @param id the id of the friendship to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/friendships/{id}")
    @Timed
    public ResponseEntity<Void> deleteFriendship(@PathVariable Long id) {
        log.debug("REST request to delete Friendship : {}", id);
        friendshipRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("friendship", id.toString())).build();
    }

}
