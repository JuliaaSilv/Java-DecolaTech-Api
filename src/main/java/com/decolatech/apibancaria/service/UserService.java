package com.decolatech.apibancaria.service;

import com.decolatech.apibancaria.domain.interfaces.service.IUserService;
import com.decolatech.apibancaria.domain.model.FinancialGoal;
import com.decolatech.apibancaria.domain.model.News;
import com.decolatech.apibancaria.domain.response.ApiResponse;
import com.decolatech.apibancaria.domain.response.ErrorResponse;
import com.decolatech.apibancaria.dto.read.UserDTO;
import com.decolatech.apibancaria.mapper.*;
import com.decolatech.apibancaria.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.decolatech.apibancaria.MethodsAdapter.AtualizarDados;

@Service
public class UserService implements IUserService {
    private final IUserRepository userRepository;
    private final INewsRepository newsRepository;
    private final ILimitManagementRepository limitManagementRepository;
    private final IFinancialGoalRepository financialGoalRepository;
    private final ICardRepository cardRepository;
    private final IAccountRepository accountRepository;

    public UserService(IUserRepository userRepository, INewsRepository newsRepository, ILimitManagementRepository limitManagementRepository, IFinancialGoalRepository financialGoalRepository, ICardRepository cardRepository, IAccountRepository accountRepository) {
        this.userRepository = userRepository;  //Injeção de dependência, chama todos os repistorys e faz um construtor deles para que os métodos acessem automaticamente o banco de dados, sem instanciar manualmente
        this.newsRepository = newsRepository;
        this.limitManagementRepository = limitManagementRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    public ApiResponse buscarUsuarios() {
        List<UserDTO> result = new ArrayList<>(); //criei uma lista para armazenar todos os objetos de USERDTO
        var users = userRepository.findAll();
        var news = newsRepository.findAll();
        var limits = limitManagementRepository.findAll();
        var financials = financialGoalRepository.findAll();
        var cards = cardRepository.findAll();
        var accounts = accountRepository.findAll();

        for (var user : users) { //cria um objeto USERDTO e preenche os dados abaixo

            UserDTO userDTO = new UserDTO();

            userDTO.id = user.getId();
            userDTO.name = user.getName();
            userDTO.cpf = user.getCpf();
            userDTO.email = user.getEmail();
            userDTO.phone = user.getPhone();
            userDTO.birthdate = user.getBirthdate();
            userDTO.password = user.getPassword();

            var account = accounts  //aqui ele utiliza o filter para filtrar os dados de cada usuário se existir, caso contrário retorna null
                    .stream().filter(x -> x.getUserId().equals(user.getId()))
                    .findFirst().orElse(null);
            var card = cards
                    .stream().filter(x -> x.getUserId().equals(user.getId()))
                    .findFirst().orElse(null);
            var limit = limits
                    .stream().filter(x -> x.getUserId().equals(user.getId()))
                    .findFirst().orElse(null);
            var financial = financials
                    .stream().filter(x -> x.getUserId().equals(user.getId()))
                    .findFirst().orElse(null);
            var notifications = news
                    .stream().filter(x -> x.getUserId().equals(user.getId()))
                    .toList();


            userDTO.account = IAccountMapper.MAP.toDto(account);
            userDTO.card = ICardMapper.MAP.toDto(card);
            userDTO.limitManagement = ILimitManagementMapper.MAP.toDto(limit);
            userDTO.financialGoal = IFinancialGoalMapper.MAP.toDto(financial);
            userDTO.news = INewsMapper.MAP.toDtoList(notifications); //Mapeamento utilizando o MAPPER, que por baixo dos panos converte os objetos para DTO

            result.add(userDTO);
        }
        return new ApiResponse(result, null, HttpStatus.OK.value());
    }

    public ApiResponse buscarUsuarioporId(Long id) {
        var result = this.buscarUsuarios().data;

        if (!(result instanceof List)) {
            return new ApiResponse(null, new ErrorResponse("Ocorreu um erro", "Erro ao converter valor em lista"), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        List<UserDTO> users = (List<UserDTO>) result;

        if (users.isEmpty()) {
            return new ApiResponse(null, new ErrorResponse("Nenhum usuário encontrado", "Não tem usuários registrados no sistema"), HttpStatus.NOT_FOUND.value());
        }
        return users
                .stream().filter(x -> x.id.equals(id))
                .findFirst()
                .map(userDTO -> new ApiResponse(userDTO, null, HttpStatus.OK.value()))
                .orElseGet(() -> new ApiResponse(null, new ErrorResponse("Usuário não encontrado", "O Id fornecido não corresponde a nenhum usuário cadastrado"), HttpStatus.NOT_FOUND.value()));

    }

    public ApiResponse DeletarUsuarioporId(Long id) {
        var result = userRepository.findById(id);
        var newsIdList = newsRepository.findByUserId(id).stream().map(News::getId).toList();
        var limitId = limitManagementRepository.findByUserId(id).getId();
        var financialIdList = financialGoalRepository.findByUserId(id).stream().map(FinancialGoal::getId).toList();
        var cardId = cardRepository.findByUserId(id).getId();
        var accountId = accountRepository.findByUserId(id).getId();


        if (result.isEmpty()) {
            return new ApiResponse(null, new ErrorResponse("Nenhum usuário encontrado", "Não tem usuários registrados no sistema"), HttpStatus.NOT_FOUND.value());


        }
        if (!newsIdList.isEmpty()) {
            for(var newsId:newsIdList) {
                newsRepository.deleteById(newsId);
            }

        }
        if (limitId >=0)
            limitManagementRepository.deleteById(limitId);

        if (!financialIdList.isEmpty()) {
            for(var financialId:financialIdList) {
                financialGoalRepository.deleteById(financialId);
            }
        }
        if (cardId >=0)
            cardRepository.deleteById(cardId);

        if (accountId >=0)
            accountRepository.deleteById(accountId);

        userRepository.deleteById(id);
        return new ApiResponse(id,null, HttpStatus.OK.value());
    }

    public ApiResponse CriarUsuario(com.decolatech.apibancaria.dto.write.UserDTO userDTO) {
        var userExists = userRepository.existsByCpf(userDTO.cpf);
        if (userExists) {
            return new ApiResponse(null, new ErrorResponse("Cpf já existe", "Cpf já registrado no banco de dados"), HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
        var user = IUserMapper.MAP.toEntityWrite(userDTO);
        var news = INewsMapper.MAP.toEntityList(userDTO.news);
        var limit = ILimitManagementMapper.MAP.toEntityWrite(userDTO.limitManagement);
        var card = ICardMapper.MAP.toEntityWrite(userDTO.card);
        var account = IAccountMapper.MAP.toEntityWrite(userDTO.account);


        var userResult = userRepository.save(user);
        var userIsCreated = !(userResult.getId() <= 0);
        if (!userIsCreated)
            return new ApiResponse(null, new ErrorResponse("Ocorreu um erro", "Não foi possível criar o usuário"), HttpStatus.INTERNAL_SERVER_ERROR.value());

        news.forEach(x -> x.setUserId(user.getId()));
        limit.setUserId(user.getId());
        card.setUserId(user.getId());
        account.setUserId(user.getId());

        newsRepository.saveAll(news);
        limitManagementRepository.save(limit);
        cardRepository.save(card);
        accountRepository.save(account);

        return new ApiResponse(userResult.getId(), null, HttpStatus.CREATED.value());
    }


    public ApiResponse AtualizarUsuario(com.decolatech.apibancaria.dto.write.UserDTO userDTO, Long id) {
        var userExists = userRepository.findById(id);
        if (userExists.isEmpty()) {
            return new ApiResponse(null, new ErrorResponse("Usuário não encontrado", "O Id fornecido não corresponde a nenhum usuário cadastrado"), HttpStatus.UNPROCESSABLE_ENTITY.value());
        }
        if (userDTO.account != null) {  //Verificando se usuário tem conta.
            userDTO.account.userId = userDTO.id;
        }

        if (userDTO.card != null) {
            userDTO.card.userId = userDTO.id;

        }

        if (userDTO.limitManagement != null) {
            userDTO.limitManagement.userId = userDTO.id;
        }

        if (!userDTO.news.isEmpty()) {
            userDTO.news.forEach(x -> x.userId = userDTO.id);
        }

        return userRepository.findById(id)      //Busca
                .map(oldUser -> { //Se o usuário for encontrado


                    var oldUserDto = IUserMapper.MAP.toDtoWrite(oldUser); //Transforma oldUser para dto
                    var newUser = AtualizarDados(oldUserDto, userDTO); //Atualiza
                    var userResult = IUserMapper.MAP.toEntityWrite(newUser); //Converte o DTO de volta

                    var accountEntity = IAccountMapper.MAP.toEntityWrite(userDTO.account); //Converte objetos para entidades.
                    var cardEntity = ICardMapper.MAP.toEntityWrite(userDTO.card);
                    var limitEntity = ILimitManagementMapper.MAP.toEntityWrite(userDTO.limitManagement);
                    var newsEntities = userDTO.news.stream().map(INewsMapper.MAP::toEntity).toList();


                    if (accountEntity != null) {
                        var oldDataAccount = accountRepository.findByUserId(id);
                        var currentAccountId = accountEntity.getId();
                        if (!currentAccountId.equals(oldDataAccount.getId()))
                            new ApiResponse(null, new ErrorResponse("Conta não encontrada", "O Id a conta não foi encontrado"), HttpStatus.NOT_FOUND.value());
                        accountRepository.save(accountEntity);
                    }
                    if (cardEntity != null) { //Verifica se possui card
                        var oldDataCard = cardRepository.findByUserId(id); //Busca card atual
                        var currentCardId = cardEntity.getId(); //Novos dados
                        if (!currentCardId.equals(oldDataCard.getId()))
                            new ApiResponse(null, new ErrorResponse("Cartão não encontrado", "O Id do cartão não foi encontrado"), HttpStatus.NOT_FOUND.value());
                        cardRepository.save(cardEntity);
                    }

                    if (limitEntity != null) {
                        var oldDataLimit = limitManagementRepository.findByUserId(id);
                        var currentLimitId = limitEntity.getId();
                        if (!currentLimitId.equals(oldDataLimit.getId()))
                            new ApiResponse(null, new ErrorResponse("Gereciamento de limite não encontrado", "O Id do gereciamento de limite não foi encontrado"), HttpStatus.NOT_FOUND.value());
                        limitManagementRepository.save(limitEntity);


                    }
                    if (!newsEntities.isEmpty()) {
                        var oldDataNews = newsRepository.findByUserId(id);
                        for (News newsEntity : newsEntities) {
                            if (newsEntity.getId() != null && oldDataNews.stream()
                                    .noneMatch(existing -> existing.getId().equals(newsEntity.getId()))) {
                                new ApiResponse(null, new ErrorResponse("Notificação não encontrada", "O Id da notificação não foi encontrado"), HttpStatus.NOT_FOUND.value());

                            }
                            newsRepository.save(newsEntity);

                        }
                    }
                    userRepository.save(userResult);
                    return new ApiResponse(id, null, HttpStatus.OK.value());

                })
                .orElseGet(() -> new ApiResponse(null, new ErrorResponse("Usuário não encontrado", "O Id fornecido não corresponde a nenhum usuário cadastrado"), HttpStatus.UNPROCESSABLE_ENTITY.value()));
    }

}
