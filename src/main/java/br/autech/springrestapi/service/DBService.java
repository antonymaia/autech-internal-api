package br.autech.springrestapi.service;

import br.autech.springrestapi.model.*;
import br.autech.springrestapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DBService {

    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EstadoRepository estadoRepository;
    @Autowired
    private CidadeRepositoy cidadeRepositoy;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder encoder;

    public void popularBancoDados() {
        Usuario usuario1 = new Usuario(null, "antony2", encoder.encode("a123"), LocalDateTime.now(), null);
        List<Usuario> usuarios = Arrays.asList(usuario1);
        usuarioRepository.saveAll(usuarios);

        Estado estado1 = new Estado(null, "PERNAMBUCO", "PE", "26");
        Estado estado2 = new Estado(null, "PARAÍBA", "PE", "25");
        List<Estado> estados = Arrays.asList(estado1, estado2);
        estadoRepository.saveAll(estados);
        estado1.setId(1L);
        estado2.setId(2L);

        Cidade cidade1 = new Cidade(null, "RECIFE", "2611606", estado1);
        Cidade cidade2 = new Cidade(null, "PITIMBU", "2511905", estado2);
        List<Cidade> cidades = Arrays.asList(cidade1, cidade2);
        cidadeRepositoy.saveAll(cidades);
        cidade1.setId(1L);
        cidade2.setId(2L);

        Endereco endereco1 = new Endereco(null, "51030000", "Avenida Boa Viagem", "Boa Viagem", cidade1);
        Endereco endereco2 = new Endereco(null, "58324000", "R. Antônio Taváres", "Centro", cidade2);
        Endereco endereco3 = new Endereco(null, "51030000", "Rua Vitor Boa lilo", "Imbiribeira", cidade1);
        Endereco endereco4 = new Endereco(null, "51030000", "Ab", "Imbiribeira", cidade1);
        Endereco endereco5 = new Endereco(null, "51030000", "Abc", "Imbiribeira", cidade1);
        List<Endereco> enderecos = Arrays.asList(endereco1, endereco2, endereco3, endereco4, endereco5);
        enderecoRepository.saveAll(enderecos);
        endereco1.setId(1L);
        endereco2.setId(2L);

      /*  Cliente cliente1 = new Cliente(
                "12987285418",
                "ANTONY MAIA",
                "81983445854",
                "ANTONYMAIATI@GMAIL.COM",
                "S",
                "25",
                "N",
                "5212",
                "APTO 314",
                endereco1);
        Cliente cliente2 = new Cliente(
                "11098541000122",
                "CACTUS SNEAKERS C.",
                "8133221155",
                "CACTUS@GMAIL.COM",
                "S",
                "02",
                "S",
                "20",
                "",
                endereco2);
        List<Cliente> clientes = Arrays.asList(cliente1,cliente2);
        clienteRepository.saveAll(clientes); */
    }
}
