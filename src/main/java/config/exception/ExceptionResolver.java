package config.exception;

import config.annotations.Component;
import config.annotations.ExceptionHandler;
import exception.*;
import http.response.ResponseCode;
import message.ExceptionMessage;

import java.lang.reflect.InvocationTargetException;

@Component
public class ExceptionResolver implements ExceptionProcessor{

    public String handleUndefinedException(Exception e) {
        return ResponseCode.BAD_REQUEST.getMessage();
    }

    @ExceptionHandler(disposeOf = InvocationTargetException.class)
    public String handleInvocationTargetException(InvocationTargetException e) {
        return e.getCause().getMessage();
    }

    @ExceptionHandler(disposeOf = AlreadyLoggedInException.class)
    public String handleAlreadyLoggedInException(AlreadyLoggedInException e) {
        return ExceptionMessage.ALREADY_LOGGED_IN;
    }

    @ExceptionHandler(disposeOf = NotLoggedInException.class)
    public String handleNotLoggedInException(NotLoggedInException e) {
        return ExceptionMessage.NOT_LOGGED_IN;
    }

    @ExceptionHandler(disposeOf = BadRequestException.class)
    public String handleBadRequestException(BadRequestException e) {
        return ExceptionMessage.BAD_REQUEST;
    }

    @ExceptionHandler(disposeOf = InvalidCredentialsException.class)
    public String handleInvalidCredentialsException(InvalidCredentialsException e) {
        return ExceptionMessage.INVALID_CREDENTIALS;
    }

    @ExceptionHandler(disposeOf = NoMatchingHandlerException.class)
    public String handleNoMatchingHandlerException(NoMatchingHandlerException e) {
        return ExceptionMessage.UNSUPPORTED_HTTP_METHOD;
    }

    @ExceptionHandler(disposeOf = UnauthorizedAccessException.class)
    public String handleUnauthorizedAccessException(UnauthorizedAccessException e) {
        return ResponseCode.UNAUTHORIZED.getMessage();
    }

    @ExceptionHandler(disposeOf = UnsupportedHttpMethod.class)
    public String handleUnsupportedHttpMethod(UnsupportedHttpMethod e) {
        return ExceptionMessage.UNSUPPORTED_HTTP_METHOD;
    }

    @ExceptionHandler(disposeOf = BoardNameAlreadyExistsException.class)
    public String handleBoardNameAlreadyExistsException(BoardNameAlreadyExistsException e) {
        return ExceptionMessage.ALREADY_USED_BOARD_NAME;
    }

    @ExceptionHandler(disposeOf = MemberIdAlreadyExistsException.class)
    public String handleMemberIdAlreadyExistsException(MemberIdAlreadyExistsException e) {
        System.out.println("ExceptionResolver.handleMemberIdAlreadyExistsException");
        return ExceptionMessage.MEMBER_ID_ALREADY_EXISTS;
    }

    @ExceptionHandler(disposeOf = MemberEmailAlreadyExistsException.class)
    public String handleMemberEmailAlreadyExistsException(MemberEmailAlreadyExistsException e) {
        return ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS;
    }

    @ExceptionHandler(disposeOf = MemberNotFoundException.class)
    public String handleMemberNotFoundException(MemberNotFoundException e) {
        return ExceptionMessage.MEMBER_NOT_FOUND;
    }

    @ExceptionHandler(disposeOf = NotFoundBoardWithBoardIdException.class)
    public String handleNotFoundBoardWithBoardIdException(NotFoundBoardWithBoardIdException e) {
        return ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_ID;
    }

    @ExceptionHandler(disposeOf = NotFoundPostWithPostIdException.class)
    public String handleNotFoundPostWithPostIdException(NotFoundPostWithPostIdException e) {
        return ExceptionMessage.NOT_FOUND_POST_WITH_POST_ID;
    }

    @ExceptionHandler(disposeOf = NotFoundBoardWithBoardNameException.class)
    public String handleNotFoundBoardWithBoardNameException(NotFoundBoardWithBoardNameException e) {
        return ExceptionMessage.NOT_FOUND_BOARD_WITH_BOARD_NAME;
    }


}
