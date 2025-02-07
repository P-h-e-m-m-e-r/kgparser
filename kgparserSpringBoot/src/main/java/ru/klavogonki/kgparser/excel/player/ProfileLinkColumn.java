package ru.klavogonki.kgparser.excel.player;

import lombok.extern.log4j.Log4j2;
import ru.klavogonki.kgparser.excel.ExcelExportContext;
import ru.klavogonki.kgparser.http.UrlConstructor;
import ru.klavogonki.kgparser.jsonParser.dto.PlayerDto;

import java.util.function.Function;

@Log4j2
public class ProfileLinkColumn implements PlayerColumn<String> {

    @Override
    public String getColumnName() {
        return "Профиль";
    }

    @Override
    public int getColumnWidth() {
        return 3000;
    }

    @Override
    public Function<PlayerDto, String> playerFieldGetter() {
        return PlayerDto::getProfileLink;
    }

    @Override
    public Class<String> fieldClass() {
        return String.class;
    }

    @Override
    public void formatCell(final ExcelExportContext context) {
        Integer playerId = context.player.getPlayerId();
        if (playerId == null) {
            logger.warn("Player with id = {}, login = \"{}\" has no playerId. Cannot add a hyperlink to this player.", playerId, context.player.getLogin());

            context.setTextAlignRightStyle(); // since profileIds are numbers, align right // todo: think about this
            context.cell.setCellValue("—");
            return;
        }

        // use a special link with no # in it, since Excel has a bug with links containing #
        String profileLink = UrlConstructor.userProfileLinkWithNoHash(playerId);
        context.setIntegerHyperlink(profileLink, playerId); // todo: think about this,this will also set link integer format
    }
}
