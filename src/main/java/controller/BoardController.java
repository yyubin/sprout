package controller;

import config.annotations.Controller;
import config.annotations.Requires;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import domain.grade.MemberGrade;
import dto.BoardRegisterDTO;
import dto.BoardUpdateDTO;
import http.request.HttpRequest;
import http.response.HttpResponse;
import http.response.ResponseCode;
import message.PrintResultMessage;
import service.BoardService;
import view.PrintHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@Requires(dependsOn = {BoardService.class, PrintHandler.class})
public class BoardController implements ControllerInterface{

    private final BoardService boardService;
    private final PrintHandler printHandler;

    public BoardController(BoardService boardService, PrintHandler printHandler) {
        this.boardService = boardService;
        this.printHandler = printHandler;
    }

    @PostMapping(path = "/boards/add")
    public void addBoard(HttpRequest<Map<String, Object>> request) throws Exception {
        Map<String, Object> body = request.getBody();
        List<MemberGrade> gradeList = new ArrayList<>();
        gradeList.add(MemberGrade.ADMIN);

        String grades = body.get("grades").toString();
        if (grades.equals(MemberGrade.USER.getDescription()) || grades.equals(MemberGrade.USER.getDescriptionEn())) {
            gradeList.add(MemberGrade.USER);
        }

        BoardRegisterDTO boardRegisterDTO = new BoardRegisterDTO(
                (String) body.get("boardName"),
                (String) body.get("description"),
                gradeList
        );
        boardService.createBoard(boardRegisterDTO);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_CREATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @PutMapping(path = "/boards/edit")
    public void editBoard(HttpRequest<Map<String, Object>> request) throws Exception {
        Long id = Long.valueOf(request.getQueryParams().get("boardId"));
        Map<String, Object> body = request.getBody();

        List<MemberGrade> gradeList = new ArrayList<>();
        gradeList.add(MemberGrade.ADMIN);

        String grades = body.get("grades").toString();
        if (grades.equals(MemberGrade.USER.getDescription()) || grades.equals(MemberGrade.USER.getDescriptionEn())) {
            gradeList.add(MemberGrade.USER);
        }

        BoardUpdateDTO boardUpdateDTO = new BoardUpdateDTO(
                id,
                (String) body.get("boardName"),
                (String) body.get("description"),
                gradeList
        );
        boardService.updateBoard(boardUpdateDTO);

        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_UPDATE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

    @DeleteMapping(path = "/boards/remove")
    public void removeBoard(HttpRequest<Map<String, Object>> request) throws Exception {
        Long id = Long.valueOf(request.getQueryParams().get("boardId"));
        boardService.deleteBoard(id);
        HttpResponse<?> response = new HttpResponse<>(
                PrintResultMessage.BOARD_DELETE_SUCCESS,
                ResponseCode.SUCCESS,
                null
        );
        printHandler.printSuccessWithResponseCodeAndCustomMessage(response);
    }

//    @GetMapping(path = "/boards/view")
//    public void viewBoard(HttpRequest<Map<String, Object>> request) throws Exception {
//        String boardName = request.getQueryParams().get("boardName");
//        boardService.deleteBoard(boardName);
//        HttpResponse<?> response = new HttpResponse<>(
//                PrintResultMessage.B
//        )
//    }

}
