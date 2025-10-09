package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Endereco;
import br.autech.springrestapi.model.Estado;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.EstadoRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.DataIntegretyException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EnderecoService enderecoService;
    private final EstadoService estadoService;
    private final CidadeService cidadeService;
    private final JavaMailSender mailSender;
    private final Environment environment;
    private final EstadoRepository estadoRepository;
    private final TemplateBuilder templateBuilder;

    public Cliente insert(Cliente cliente) {
        if (clienteRepository.findByCnpjCpf(cliente.getCnpjCpf()).isPresent()) {
            throw new DataIntegretyException("CNPJ/CPF Já cadastrado");
        }

        if (cliente.getEndereco() != null) {
            if (cliente.getEndereco().getId() != null) {
                cliente.setEndereco(enderecoService.findById(cliente.getEndereco().getId()));

            } else {
                Cidade cidade = cidadeService.findByCodigoMunicipio(cliente.getEndereco().getCidade().getCodigoMunicipio());

                if (cidade == null) {
                    cidade = cliente.getEndereco().getCidade();

                    Estado estado = estadoRepository.findByCodigoUf(
                            cliente.getEndereco().getCidade().getEstado().getCodigoUf()
                    ).orElse(null);

                    if (estado == null) {
                        estado = estadoService.insert(cliente.getEndereco().getCidade().getEstado());
                    }
                    cidade.setEstado(estado);
                    cidade = cidadeService.insert(cidade);
                }

                Endereco endereco = enderecoService.findByLogradouroAndBairroAndCidade(
                        cliente.getEndereco().getLogradouro(),
                        cliente.getEndereco().getBairro(),
                        cidade);


                if (endereco == null) {
                    endereco = cliente.getEndereco();
                    endereco.setCidade(cidade);
                    endereco.setLogradouro(enderecoService.padronizacaoLogradouro(endereco.getLogradouro()));
                    endereco = enderecoService.create(endereco);
                }
                cliente.setEndereco(endereco);
            }
        }
        cliente.setAtivo("S");
        cliente.setBloqueado("N");
        return clienteRepository.save(cliente);
    }

    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    public Cliente findByCnpjCpf(String cnpjCpf) {
        Optional<Cliente> cliente = clienteRepository.findByCnpjCpf(cnpjCpf);
        return cliente.orElseThrow(() -> new ObjectNotFoundException("Cliente não encontrado na base de dados, CNPJ/CPF: " + cnpjCpf));
    }

    public void delete(String cnpjCpf) {
        clienteRepository.deleteById(cnpjCpf);
    }

    public Page<Cliente> search(int searchId, String searchTerm, int page, int size) {


        searchTerm = "%" + searchTerm + "%";

        if (searchId == 1) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("CNPJ_CPF"));
            return clienteRepository.searchByCnpjCpf(searchTerm, pageable);
        }
        if (searchId == 2) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nome"));
            return clienteRepository.searchByNome(searchTerm.toUpperCase(), pageable);
        }
        if (searchId == 3) {
            Pageable pageable = PageRequest.of(page, size);
            return clienteRepository.findAllByBairro(searchTerm, pageable);
        }
        if (searchId == 4) {
            Pageable pageable = PageRequest.of(page, size);
            return clienteRepository.findAllByCidade(searchTerm, pageable);
        }

        return null;
    }

    public Cliente update(Cliente cliente) {
        findByCnpjCpf(cliente.getCnpjCpf());

        if (cliente.getEndereco() != null && cliente.getEndereco().getCep().isEmpty())
            throw new BadRequestException("Informe o CEP do endereço");

        if (cliente.getEndereco() != null && cliente.getEndereco().getId() != null) {

            cliente.getEndereco().setLogradouro(enderecoService.padronizacaoLogradouro(cliente.getEndereco().getLogradouro()));

            Endereco enderecoCadastrado = enderecoService.findById(cliente.getEndereco().getId());

            if (!enderecoCadastrado.getLogradouro().equalsIgnoreCase(cliente.getEndereco().getLogradouro()) ||
                    !enderecoCadastrado.getBairro().equalsIgnoreCase(cliente.getEndereco().getBairro())) {
                cliente.getEndereco().setId(null);
                Endereco novoEndereco = enderecoService.create(cliente.getEndereco());
                cliente.setEndereco(novoEndereco);
            }
            return clienteRepository.save(cliente);
        }

        if (cliente.getEndereco() != null) {
            cliente.getEndereco().setLogradouro(enderecoService.padronizacaoLogradouro(cliente.getEndereco().getLogradouro()));

            Cidade cidade = cidadeService.findByCodigoMunicipio(cliente.getEndereco().getCidade().getCodigoMunicipio());
            if (cidade == null) {
                cidade = cliente.getEndereco().getCidade();
                Estado estado = estadoService.findByCodigoUF(cliente.getEndereco().getCidade().getEstado().getCodigoUf());
                if (estado == null) {
                    estado = estadoService.insert(cliente.getEndereco().getCidade().getEstado());
                }
                cidade.setEstado(estado);
                cidade = cidadeService.insert(cidade);
            }

            Endereco endereco = enderecoService.findByLogradouroAndBairroAndCidade(
                    cliente.getEndereco().getLogradouro(),
                    cliente.getEndereco().getBairro(),
                    cidade);

            if (endereco == null) {
                endereco = cliente.getEndereco();
                endereco.setCidade(cidade);
                endereco = enderecoService.create(endereco);
            }

            cliente.setEndereco(endereco);
        }

        return clienteRepository.save(cliente);
    }

    public JSONObject getChave(String cnpjCpf) {
        int cnpjCpfParte1, cnpjCpfParte2, cnpjCpfParte3, cnpjCpfParte4, calculoCnpjCpf,calculoA,
                calculoB, calculoC, calculoData, chave;

        if (cnpjCpf.length() < 11 || cnpjCpf.length() > 14) {
            throw new DataIntegretyException("CNPJ/CPF inválido, o número deve ser maior que 10 e menor que 15 caracteres");
        }

        findByCnpjCpf(cnpjCpf);

        if (cnpjCpf.length() == 11) cnpjCpf += "111";

        cnpjCpfParte1 = Integer.parseInt(cnpjCpf.substring(0, 2));
        cnpjCpfParte2 = Integer.parseInt(cnpjCpf.substring(2, 5));
        cnpjCpfParte3 = Integer.parseInt(cnpjCpf.substring(5, 8));
        cnpjCpfParte4 = Integer.parseInt(cnpjCpf.substring(12, 14));
        calculoCnpjCpf = (cnpjCpfParte1 + cnpjCpfParte2 + cnpjCpfParte3) * cnpjCpfParte4;

        Calendar data = Calendar.getInstance();

        calculoA = cnpjCpfParte1 * data.get(GregorianCalendar.DAY_OF_MONTH);
        calculoB = (cnpjCpfParte2 * (data.get(GregorianCalendar.MONTH) + 1)) + calculoA;
        calculoC = (cnpjCpfParte3 * data.get(GregorianCalendar.YEAR)) + calculoB;
        calculoData = calculoA + calculoB + calculoC;

        chave = calculoCnpjCpf + calculoData;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("chave", chave);

        return jsonObject;
    }

    public Cliente findById(String id) {
        return clienteRepository.findById(id).orElseThrow(
                () -> new ObjectNotFoundException("Cliente não encontrado, id: " + id)
        );
    }

    public List<Cliente> buscarClientesPorDiaVencimentoAtivo(int diaVencimento, String ativo){
        if(diaVencimento < 10){
            return clienteRepository
                    .findAllByDiaVencimentoAndAtivoAndEmailNotNull("0" + diaVencimento, ativo);
        }
        return clienteRepository.findAllByDiaVencimentoAndAtivoAndEmailNotNull(""+diaVencimento, ativo);
    }

    public void enviarEmailLicencaVencida(Cliente cliente) {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dataVencimento = localDate.format(formatter);

        Map<String, Object> variables = new HashMap<>();
        variables.put("cliente", cliente);
        variables.put("dataVencimento", dataVencimento);

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(Objects.requireNonNull(environment.getProperty("spring.mail.username")));
            messageHelper.setTo(cliente.getEmail());
            messageHelper.setSubject("Mensalidade Autech");
            messageHelper.setText(templateBuilder.build("email-licenca-vencida", variables), true);
        };
        try {
            mailSender.send(messagePreparator);
        } catch (MailException e){
            System.out.println(e.getMessage());
        }
    }

    public Long countClientesByAtivo(String ativo) {
        if(!ativo.equalsIgnoreCase("S") && !ativo.equalsIgnoreCase("N"))
            throw new BadRequestException("Valor inválido, utilize 'S'ou 'N'");

        return clienteRepository.countByAtivo(ativo);
    }

    public Long countClientesByBloqueado(String bloqueado) {
        if(!bloqueado.equalsIgnoreCase("S") && !bloqueado.equalsIgnoreCase("N"))
            throw new BadRequestException("Valor inválido, utilize 'S'ou 'N'");

        return clienteRepository.countByBloqueadoAndAtivo(bloqueado, "S");
    }

    public List<Cliente> buscarClientesPorDiaVencimentoa5dias() {
        LocalDate diatAtual = LocalDate.now();
        String diaVencimento = (diatAtual.getDayOfMonth() + 5 ) < 10 ? "0" + (diatAtual.getDayOfMonth() + 5 ) : Integer.toString(diatAtual.getDayOfMonth() + 5 );

        return clienteRepository.findAllByDiaVencimentoAndAtivo(diaVencimento, "S");
    }

}
