# Terracotta PII helpers: piiencrypt, piidecrypt, piihashemail
#
# Each one prompts for the Terracotta jar and an environment once, then keeps prompting for values
# until you type "quit", printing the result for each. The keys are only passed to the java command
# that does the work: they aren't exported, and they don't end up in your shell history.
#
# The keys for an environment are read from a PII associative array (bash 4 or later), keyed by
# environment and key name, if you define one in your ~/.bashrc:
#
#   declare -A PII=(
#     [local,PII_ENCRYPTION_KEY]="..."   [local,PII_HASH_KEY]="..."
#     [qa,PII_ENCRYPTION_KEY]="..."      [qa,PII_HASH_KEY]="..."
#     [prod,PII_ENCRYPTION_KEY]="..."    [prod,PII_HASH_KEY]="..."
#   )
#
# PII_ENCRYPTION_KEY_ID can be set per environment the same way, and defaults to 1. Any key the
# array doesn't have for that environment is prompted for instead. If PII_ENCRYPTION_KEY,
# PII_HASH_KEY or PII_ENCRYPTION_KEY_ID are already set in your shell, pressing Enter at that
# prompt uses the value that's set.

# prompts for a key without echoing it; Enter keeps the one already set in the shell, if any
_pii_prompt_key() {
    local name="$1" current="$2" typed

    if [ -n "$current" ]; then
        printf '%s (press Enter to use the one already set): ' "$name" >&2
    else
        printf '%s: ' "$name" >&2
    fi

    IFS= read -rs typed
    echo >&2
    printf '%s' "${typed:-$current}"
}

# prints PII[environment,name], or nothing if there's no PII associative array or no such entry;
# the check keeps bash 3.2, which has no associative arrays, from reading the subscript as math
_pii_lookup() {
    local environment="$1" name="$2"

    [ -n "$environment" ] || return 0
    [[ "$(declare -p PII 2>/dev/null)" == "declare -A"* ]] || return 0

    printf '%s' "${PII[$environment,$name]}"
}

_pii_run() {
    local command="$1" jar environment key hash_key key_id value

    # a jar file, or a directory holding one (the newest terracotta*.jar in it is used)
    echo "Terracotta jar, or the directory it's in. Press Enter for $PWD/target" >&2
    read -rep "Jar: " jar
    jar="${jar:-$PWD/target}"

    if [ -d "$jar" ]; then
        jar="$(ls -t "$jar"/terracotta*.jar 2>/dev/null | head -1)"
    fi

    if [ ! -f "$jar" ]; then
        echo "No Terracotta jar found there." >&2
        return 2
    fi

    echo "Using $jar" >&2

    read -rep "Environment (local, qa, prod): " environment

    key="$(_pii_lookup "$environment" PII_ENCRYPTION_KEY)"
    hash_key="$(_pii_lookup "$environment" PII_HASH_KEY)"
    key_id="$(_pii_lookup "$environment" PII_ENCRYPTION_KEY_ID)"

    if [ -n "$key" ] && [ -n "$hash_key" ]; then
        echo "Using the $environment keys from PII." >&2
    else
        echo "Enter the keys Terracotta uses for ${environment:-this environment} (nothing you type is shown)." >&2
    fi

    [ -n "$key" ] || key="$(_pii_prompt_key PII_ENCRYPTION_KEY "$PII_ENCRYPTION_KEY")"
    [ -n "$hash_key" ] || hash_key="$(_pii_prompt_key PII_HASH_KEY "$PII_HASH_KEY")"

    if [ -z "$key" ] || [ -z "$hash_key" ]; then
        echo "Both keys are needed." >&2
        return 2
    fi

    # an environment found in PII without a key id uses the default without asking
    if [ -z "$key_id" ] && [ -n "$(_pii_lookup "$environment" PII_ENCRYPTION_KEY)" ]; then
        key_id="${PII_ENCRYPTION_KEY_ID:-1}"
    elif [ -z "$key_id" ]; then
        printf 'PII_ENCRYPTION_KEY_ID (press Enter for %s): ' "${PII_ENCRYPTION_KEY_ID:-the default, 1}" >&2
        IFS= read -r key_id
        key_id="${key_id:-${PII_ENCRYPTION_KEY_ID:-1}}"
    fi

    echo >&2
    echo "Enter a value at each prompt. Type quit to finish." >&2

    while true; do
        printf 'Value: ' >&2

        # secrets to encrypt aren't shown as you type; stored values and emails are
        if [ "$command" = encrypt ]; then
            IFS= read -rs value || break
            echo >&2
        else
            IFS= read -r value || break
        fi

        [ "$value" = quit ] && break
        [ -z "$value" ] && continue

        # the value goes in on standard input, never as a command-line argument
        printf '%s\n' "$value" | PII_ENCRYPTION_KEY="$key" PII_HASH_KEY="$hash_key" PII_ENCRYPTION_KEY_ID="$key_id" \
            java -cp "$jar" -Dloader.main=edu.iu.terracotta.security.pii.PiiCli \
            org.springframework.boot.loader.launch.PropertiesLauncher "$command"

        # 2 means a key is missing or invalid, which every later value would hit too
        if [ $? -eq 2 ]; then
            return 2
        fi
    done
}

piiencrypt() { _pii_run encrypt; }
piidecrypt() { _pii_run decrypt; }
piihashemail() { _pii_run hash-email; }
alias ppihashemail=piihashemail
