package app.exception;

import java.lang.reflect.InvocationTargetException;

public interface ExceptionProcessor {

    String handleUndefinedException(Exception e);

    String handleInvocationTargetException(InvocationTargetException e);

    String handleAlreadyLoggedInException(AlreadyLoggedInException e);

    String handleNotLoggedInException(NotLoggedInException e);

    String handleBadRequestException(BadRequestException e);

    String handleInvalidCredentialsException(InvalidCredentialsException e);

    String handleNoMatchingHandlerException(NoMatchingHandlerException e);

    String handleUnauthorizedAccessException(UnauthorizedAccessException e);

    String handleUnsupportedHttpMethod(UnsupportedHttpMethod e);

    String handleBoardNameAlreadyExistsException(BoardNameAlreadyExistsException e);

    String handleMemberIdAlreadyExistsException(MemberIdAlreadyExistsException e);

    String handleMemberEmailAlreadyExistsException(MemberEmailAlreadyExistsException e);

    String handleMemberNotFoundException(MemberNotFoundException e);

    String handleNotFoundBoardWithBoardIdException(NotFoundBoardWithBoardIdException e);

    String handleNotFoundPostWithPostIdException(NotFoundPostWithPostIdException e);

    String handleNotFoundBoardWithBoardNameException(NotFoundBoardWithBoardNameException e);
}
