import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Vyacheslav on 12/25/2017.
 */
public class Bot extends TelegramLongPollingBot {
    private HashMap<Integer, Integer> mUserAccess = new HashMap<>();
    private HashMap<Long, Integer> mBanList = new HashMap<>();

    private String mApiKey = "490552086:AAGN63CTIMYwu59REI3YF5tnsi4MhpObLPg";
    private String mNameOfBot = "ValeraSecurityManBot";
    int MAX_WRITE_TEXT = 2;

    Message securityWarningMessage = null;

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message.getLeftChatMember() != null || message.getNewChatMembers() != null) {
//            for (Map.Entry<Long, Integer> entry : mBanList.entrySet()) {
//
//            }
            deleteExtraMessageBots(message);
            return;
        }
        System.out.println(message.getText());

        if (isAdministratorMessage(message) || isTakeAcceess(message)) {

        } else {
            if (securityWarningMessage != null) {
                deleteExtraMessageBots(securityWarningMessage);
            }
            securityWarningMessage = sendMessageBot(message.getChatId(), setMessageByCount(message));
        }
    }


    @Override
    public String getBotUsername() {
        return mNameOfBot;
    }

    @Override
    public String getBotToken() {
        return mApiKey;
    }

//    private void checkAndKickBannedUser(Message message){
//
//    }

    private void kickUser(long chatId, int userId) {
        KickChatMember kickChatMember = new KickChatMember(chatId, userId);
        try {
            kickMember(kickChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String setMessageByCount(Message message) {
        if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 10) {
            mBanList.put(message.getChatId(), message.getFrom().getId());
            kickUser(message.getChatId(), message.getFrom().getId());
            return message.getFrom().getFirstName() + "удален, и так будет с каждым";
        } else if ((mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 8)) {
            return "Еще одно сообщение, и я поделю тебя на ноль";
        } else if ((mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 3)) {
            return "Слющай, еще разь так сдэляэшь, Мавроди пазваню, атвечаю";
        } else if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT) {
            return "Твое последние слово. Лимит закончен";
        }
        return "Эээ спахука";
    }

    private Message sendMessageBot(long ChatId, String text) {
        try {
            return sendMessage(new SendMessage(ChatId, text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteExtraMessageBots(Message message) {
        DeleteMessage deleteMessage = new DeleteMessage(message.getChatId(), message.getMessageId());
        try {
            deleteMessage(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isAdministratorMessage(Message message) {
//        List<ChatMember> chatMembers = getAdministrator(message);
//        for (ChatMember chatMember : chatMembers) {
//            if (chatMember.getUser().getId().intValue() == message.getFrom().getId()) {
//                return true;
//            }
//        }
        return false;
    }

    private List<ChatMember> getAdministrator(Message message) {
        GetChatAdministrators getChatAdministrator = new GetChatAdministrators();
        getChatAdministrator.setChatId(message.getChatId());
        try {
            return getChatAdministrators(getChatAdministrator);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return new ArrayList<ChatMember>();
    }

    private boolean isTakeAcceess(Message message) {
        boolean result = false;
        if (mUserAccess.containsKey(message.getFrom().getId())) {
            mUserAccess.put(message.getFrom().getId(), mUserAccess.get(message.getFrom().getId()) + 1);
            if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT) {
                result = false;
                deleteExtraMessageBots(message);
            } else {
                result = true;
            }
        } else {
            mUserAccess.put(message.getFrom().getId(), 1);
            result = true;
        }
        return result;
    }
}
