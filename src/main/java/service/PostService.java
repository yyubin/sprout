package service;

import domain.Board;
import domain.Member;
import domain.Post;
import dto.PostRegisterDTO;
import repository.InMemoryPostRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class PostService {

    private final InMemoryPostRepository postRepository;
    private final MemberService memberService;
    private final MemberAuthService memberAuthService;

    public PostService(InMemoryPostRepository postRepository, MemberService memberService, MemberAuthService memberAuthService) {
        this.postRepository = postRepository;
        this.memberService = memberService;
        this.memberAuthService = memberAuthService;
    }

//    public void createPost(PostRegisterDTO postRegisterDTO, String sessionId) {
//        String memberId = memberAuthService.getRedisSessionManager().getSession(sessionId);
//        Optional<Member> author = memberService.getMemberById(memberId);
//        Post post = new Post(
//                postRegisterDTO.getPostName(),
//                postRegisterDTO.getPostContent(),
//                author,
//                postRegisterDTO.getBoardId(),
//                LocalDateTime.now()
//        );
//    }


}
