package br.com.nlw.events.service;

import br.com.nlw.events.dto.SubscriptionRankingByUser;
import br.com.nlw.events.dto.SubscriptionRankingItem;
import br.com.nlw.events.dto.SubscriptionResponse;
import br.com.nlw.events.exception.SubscriptionConflictException;
import br.com.nlw.events.exception.EventNotFoundException;
import br.com.nlw.events.exception.UserIndicadorNotFoundException;
import br.com.nlw.events.model.Event;
import br.com.nlw.events.model.Subscription;
import br.com.nlw.events.model.User;
import br.com.nlw.events.repository.EventRepo;
import br.com.nlw.events.repository.SubscriptionRepo;
import br.com.nlw.events.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class SubscriptionService {

    @Autowired
    private EventRepo evtRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private SubscriptionRepo subRepo;

    public SubscriptionResponse CreateNewSubscription(String eventName, User user, Integer userId) {

        //recuperar o evento pelo nome
        Event evt = evtRepo.findByPrettyName(eventName);
        if (evt == null) { // caso alternativo 2
            throw new EventNotFoundException("Evento " + eventName + " não existe");
        }

        User userRecovered = userRepo.findByEmail(user.getEmail());
        if (userRecovered == null) { // caso alternativo 1
            userRecovered = userRepo.save(user);
        }
        User indicador = null;
        if (userId != null) {
            indicador = userRepo.findById(userId).orElse(null);
            if (indicador == null) {
                throw new UserIndicadorNotFoundException("Usuário " + userId + " indicador não existe");
            }
        }
        Subscription subs = new Subscription();
        subs.setEvent(evt);
        subs.setSubscriber(userRecovered);
        subs.setIndication(indicador);

        Subscription tmpSub = subRepo.findByEventAndSubscriber(evt, userRecovered);
        if (tmpSub != null) { // caso alternativo 3
            throw new SubscriptionConflictException("Já existe inscrição para  usuário " + userRecovered.getName() + " no evento " + evt.getTitle());
        }

        Subscription res = subRepo.save(subs);
        return new SubscriptionResponse(res.getSubscriptionNumber(), "https://codecraft.com/subscription/"+res.getEvent().getPrettyName()+"/"+res.getSubscriber().getId());
    }


    public List<SubscriptionRankingItem> getCompleteRanking(String prettyName){
        Event evt = evtRepo.findByPrettyName(prettyName);
        if (evt == null) {
            throw new EventNotFoundException("Ranking do evento " + prettyName + " não existe");
        }
        return subRepo.generateRanking(evt.getEventId());
    }

    public SubscriptionRankingByUser getRankingByUser(String prettyName, Integer userId) {
        List<SubscriptionRankingItem> ranking = getCompleteRanking(prettyName);
        SubscriptionRankingItem item = ranking.stream().filter(i->i.userId().equals(userId)).findFirst().orElse(null);
        if (item == null) {
            throw new UserIndicadorNotFoundException("Não há inscrições com indicação do usuario " + userId);
        }
        Integer posicao = IntStream.range(0, ranking.size())
                .filter(pos -> ranking.get(pos).userId().equals(userId))
                .findFirst().getAsInt();

        return new SubscriptionRankingByUser(item, posicao + 1);
    }
}
