package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import dto.MemberRegisterDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import service.MemberService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@Requires(dependsOn = {MemberService.class})
public class MemberController implements ControllerInterface {

    private final MemberService memberService;
    private final JSONParser jsonParser = new JSONParser();

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping(path = "/accounts/signup")
    public HttpResponse<?> signup(HttpRequest<Map<String, Object>> request) {
        Map<String, Object> body = request.getBody();
        MemberRegisterDTO memberRegisterDTO = new MemberRegisterDTO(
                (String) body.get("id"),
                (String) body.get("name"),
                (String) body.get("email"),
                (String) body.get("password"),
                LocalDate.now()
        );
        memberService.registerMember(memberRegisterDTO);
        System.out.println(memberService.getAllMembers());
        return new HttpResponse<>();
    }

    @PostMapping(path = "/accounts/signin")
    public void signin() {

    }

    @GetMapping(path = "/accounts/signout")
    public void signout() {

    }

    @GetMapping(path = "/accounts/detail")
    public void detail() {

    }

    @PutMapping(path = "/accounts/edit")
    public void edit() {

    }

    @DeleteMapping(path = "/accounts/remove")
    public void remove() {

    }

}
