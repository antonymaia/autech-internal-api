package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Produto;
import br.autech.springrestapi.repository.ProdutoRepository;
import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public List<Produto> findAll() {
        return produtoRepository.findAll();
    }

    public Produto findById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Produto " + id + " nao encontrado"));
    }

    public Produto criar(Produto produto) {
        validar(produto);
        produto.setIdProduto(null);
        aplicarNormalizacao(produto);
        return produtoRepository.save(produto);
    }

    public Produto atualizar(Long id, Produto produto) {
        Produto existente = findById(id);
        validar(produto);
        existente.setNome(produto.getNome().trim().toUpperCase());
        existente.setDescricao(produto.getDescricao() != null ? produto.getDescricao().trim() : null);
        existente.setValor(produto.getValor());
        return produtoRepository.save(existente);
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new BadRequestException("nome obrigatorio");
        }
        if (produto.getValor() == null) {
            throw new BadRequestException("valor obrigatorio");
        }
        if (produto.getValor().signum() < 0) {
            throw new BadRequestException("valor nao pode ser negativo");
        }
    }

    private void aplicarNormalizacao(Produto produto) {
        produto.setNome(produto.getNome().trim().toUpperCase());
        if (produto.getDescricao() != null) {
            produto.setDescricao(produto.getDescricao().trim());
        }
    }

    public List<Produto> search(int searchId, String searchTerm) {
        if (searchTerm == null) searchTerm = "";
        String termo = searchTerm.trim();

        switch (searchId) {
            case 1:
                try {
                    return produtoRepository.findById(Long.parseLong(termo))
                            .map(Collections::singletonList)
                            .orElse(Collections.emptyList());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("searchTerm deve ser numerico quando searchId=1");
                }
            case 2:
                return produtoRepository.findByNomeStartingWithIgnoreCaseOrderByNome(termo);
            case 3:
                return produtoRepository.findByNomeContainingIgnoreCaseOrderByNome(termo);
            default:
                throw new BadRequestException("searchId invalido. Use 1 (id), 2 (nome comecando) ou 3 (nome contendo)");
        }
    }
}
